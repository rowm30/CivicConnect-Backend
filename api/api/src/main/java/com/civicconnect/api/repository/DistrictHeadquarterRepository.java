package com.civicconnect.api.repository;

import com.civicconnect.api.entity.DistrictHeadquarter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistrictHeadquarterRepository extends JpaRepository<DistrictHeadquarter, Long> {

    List<DistrictHeadquarter> findByStateName(String stateName);

    Page<DistrictHeadquarter> findByStateName(String stateName, Pageable pageable);

    @Query("SELECT DISTINCT h.stateName FROM DistrictHeadquarter h ORDER BY h.stateName")
    List<String> findDistinctStateNames();

    @Query("SELECT h FROM DistrictHeadquarter h WHERE " +
           "(:q IS NULL OR LOWER(h.hqName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(h.districtName) LIKE LOWER(CONCAT('%', :q, '%'))) AND " +
           "(:stateName IS NULL OR h.stateName = :stateName)")
    Page<DistrictHeadquarter> searchHQs(
            @Param("q") String q,
            @Param("stateName") String stateName,
            Pageable pageable
    );

    // Get all HQs as GeoJSON
    @Query(value = """
            SELECT json_build_object(
                'type', 'FeatureCollection',
                'features', COALESCE(json_agg(
                    json_build_object(
                        'type', 'Feature',
                        'geometry', ST_AsGeoJSON(location)::json,
                        'properties', json_build_object(
                            'id', id,
                            'hqId', hq_id,
                            'hqName', hq_name,
                            'townName', town_name,
                            'districtName', district_name,
                            'stateName', state_name,
                            'talukName', taluk_name
                        )
                    )
                ), '[]'::json)
            )
            FROM district_headquarters
            """, nativeQuery = true)
    String findAllAsGeoJson();

    // Get HQs by state as GeoJSON
    @Query(value = """
            SELECT json_build_object(
                'type', 'FeatureCollection',
                'features', COALESCE(json_agg(
                    json_build_object(
                        'type', 'Feature',
                        'geometry', ST_AsGeoJSON(location)::json,
                        'properties', json_build_object(
                            'id', id,
                            'hqId', hq_id,
                            'hqName', hq_name,
                            'townName', town_name,
                            'districtName', district_name,
                            'stateName', state_name,
                            'talukName', taluk_name
                        )
                    )
                ), '[]'::json)
            )
            FROM district_headquarters
            WHERE state_name = :stateName
            """, nativeQuery = true)
    String findByStateAsGeoJson(@Param("stateName") String stateName);
}
