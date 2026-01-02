package com.civicconnect.api.repository;

import com.civicconnect.api.entity.District;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {

    Optional<District> findByDistrictId(String districtId);

    List<District> findByStateName(String stateName);

    Page<District> findByStateName(String stateName, Pageable pageable);

    @Query("SELECT DISTINCT d.stateName FROM District d ORDER BY d.stateName")
    List<String> findDistinctStateNames();

    @Query(value = """
            SELECT * FROM districts d WHERE
            (:q IS NULL OR LOWER(d.district_name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR
            LOWER(d.state_name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))) AND
            (:stateName IS NULL OR d.state_name = CAST(:stateName AS text))
            """,
           countQuery = """
            SELECT COUNT(*) FROM districts d WHERE
            (:q IS NULL OR LOWER(d.district_name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR
            LOWER(d.state_name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))) AND
            (:stateName IS NULL OR d.state_name = CAST(:stateName AS text))
            """,
           nativeQuery = true)
    Page<District> searchDistricts(
            @Param("q") String q,
            @Param("stateName") String stateName,
            Pageable pageable
    );

    // Find district by GPS coordinates
    @Query(value = """
            SELECT * FROM districts
            WHERE ST_Contains(boundary, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326))
            LIMIT 1
            """, nativeQuery = true)
    Optional<District> findByPoint(@Param("lat") double lat, @Param("lng") double lng);

    // Get all districts as GeoJSON
    @Query(value = """
            SELECT json_build_object(
                'type', 'FeatureCollection',
                'features', COALESCE(json_agg(
                    json_build_object(
                        'type', 'Feature',
                        'geometry', ST_AsGeoJSON(boundary)::json,
                        'properties', json_build_object(
                            'id', id,
                            'districtId', district_id,
                            'districtName', district_name,
                            'stateName', state_name,
                            'areaSqKm', area_sq_km
                        )
                    )
                ), '[]'::json)
            )
            FROM districts
            """, nativeQuery = true)
    String findAllAsGeoJson();

    // Get districts by state as GeoJSON
    @Query(value = """
            SELECT json_build_object(
                'type', 'FeatureCollection',
                'features', COALESCE(json_agg(
                    json_build_object(
                        'type', 'Feature',
                        'geometry', ST_AsGeoJSON(boundary)::json,
                        'properties', json_build_object(
                            'id', id,
                            'districtId', district_id,
                            'districtName', district_name,
                            'stateName', state_name,
                            'areaSqKm', area_sq_km
                        )
                    )
                ), '[]'::json)
            )
            FROM districts
            WHERE state_name = :stateName
            """, nativeQuery = true)
    String findByStateNameAsGeoJson(@Param("stateName") String stateName);

    // Get simplified GeoJSON for all-India view
    @Query(value = """
            SELECT json_build_object(
                'type', 'FeatureCollection',
                'features', COALESCE(json_agg(
                    json_build_object(
                        'type', 'Feature',
                        'geometry', ST_AsGeoJSON(ST_Simplify(boundary, :tolerance))::json,
                        'properties', json_build_object(
                            'id', id,
                            'districtId', district_id,
                            'districtName', district_name,
                            'stateName', state_name,
                            'areaSqKm', area_sq_km
                        )
                    )
                ), '[]'::json)
            )
            FROM districts
            """, nativeQuery = true)
    String findAllAsSimplifiedGeoJson(@Param("tolerance") double tolerance);
}
