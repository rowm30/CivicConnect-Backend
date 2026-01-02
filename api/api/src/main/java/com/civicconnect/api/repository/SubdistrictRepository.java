package com.civicconnect.api.repository;

import com.civicconnect.api.entity.Subdistrict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubdistrictRepository extends JpaRepository<Subdistrict, Long> {

    Optional<Subdistrict> findBySubdistrictId(String subdistrictId);

    List<Subdistrict> findByStateName(String stateName);

    List<Subdistrict> findByDistrictName(String districtName);

    Page<Subdistrict> findByStateName(String stateName, Pageable pageable);

    @Query("SELECT DISTINCT s.stateName FROM Subdistrict s ORDER BY s.stateName")
    List<String> findDistinctStateNames();

    @Query("SELECT DISTINCT s.districtName FROM Subdistrict s WHERE s.stateName = :stateName ORDER BY s.districtName")
    List<String> findDistinctDistrictsByState(@Param("stateName") String stateName);

    @Query(value = """
            SELECT * FROM subdistricts s WHERE
            (:q IS NULL OR LOWER(s.subdistrict_name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR
            LOWER(s.district_name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))) AND
            (:stateName IS NULL OR s.state_name = CAST(:stateName AS text)) AND
            (:districtName IS NULL OR s.district_name = CAST(:districtName AS text))
            """,
           countQuery = """
            SELECT COUNT(*) FROM subdistricts s WHERE
            (:q IS NULL OR LOWER(s.subdistrict_name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR
            LOWER(s.district_name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))) AND
            (:stateName IS NULL OR s.state_name = CAST(:stateName AS text)) AND
            (:districtName IS NULL OR s.district_name = CAST(:districtName AS text))
            """,
           nativeQuery = true)
    Page<Subdistrict> searchSubdistricts(
            @Param("q") String q,
            @Param("stateName") String stateName,
            @Param("districtName") String districtName,
            Pageable pageable
    );

    // Find subdistrict by GPS coordinates
    @Query(value = """
            SELECT * FROM subdistricts
            WHERE ST_Contains(boundary, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326))
            LIMIT 1
            """, nativeQuery = true)
    Optional<Subdistrict> findByPoint(@Param("lat") double lat, @Param("lng") double lng);

    // Get subdistricts by district as GeoJSON
    @Query(value = """
            SELECT json_build_object(
                'type', 'FeatureCollection',
                'features', COALESCE(json_agg(
                    json_build_object(
                        'type', 'Feature',
                        'geometry', ST_AsGeoJSON(boundary)::json,
                        'properties', json_build_object(
                            'id', id,
                            'subdistrictId', subdistrict_id,
                            'subdistrictName', subdistrict_name,
                            'subdistrictType', subdistrict_type,
                            'districtName', district_name,
                            'stateName', state_name,
                            'areaSqKm', area_sq_km
                        )
                    )
                ), '[]'::json)
            )
            FROM subdistricts
            WHERE district_name = :districtName
            """, nativeQuery = true)
    String findByDistrictAsGeoJson(@Param("districtName") String districtName);

    // Get subdistricts by state as GeoJSON (simplified)
    @Query(value = """
            SELECT json_build_object(
                'type', 'FeatureCollection',
                'features', COALESCE(json_agg(
                    json_build_object(
                        'type', 'Feature',
                        'geometry', ST_AsGeoJSON(ST_Simplify(boundary, 0.001))::json,
                        'properties', json_build_object(
                            'id', id,
                            'subdistrictId', subdistrict_id,
                            'subdistrictName', subdistrict_name,
                            'subdistrictType', subdistrict_type,
                            'districtName', district_name,
                            'stateName', state_name,
                            'areaSqKm', area_sq_km
                        )
                    )
                ), '[]'::json)
            )
            FROM subdistricts
            WHERE state_name = :stateName
            """, nativeQuery = true)
    String findByStateAsGeoJson(@Param("stateName") String stateName);
}
