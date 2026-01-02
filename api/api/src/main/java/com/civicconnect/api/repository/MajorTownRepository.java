package com.civicconnect.api.repository;

import com.civicconnect.api.entity.MajorTown;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MajorTownRepository extends JpaRepository<MajorTown, Long> {

    List<MajorTown> findByStateName(String stateName);

    List<MajorTown> findByDistrictName(String districtName);

    Page<MajorTown> findByStateName(String stateName, Pageable pageable);

    @Query("SELECT DISTINCT t.stateName FROM MajorTown t ORDER BY t.stateName")
    List<String> findDistinctStateNames();

    @Query("SELECT DISTINCT t.districtName FROM MajorTown t WHERE t.stateName = :stateName ORDER BY t.districtName")
    List<String> findDistinctDistrictsByState(@Param("stateName") String stateName);

    @Query(value = """
            SELECT * FROM major_towns t WHERE
            (:q IS NULL OR LOWER(t.town_name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR
            LOWER(t.district_name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))) AND
            (:stateName IS NULL OR t.state_name = CAST(:stateName AS text)) AND
            (:districtName IS NULL OR t.district_name = CAST(:districtName AS text))
            """,
           countQuery = """
            SELECT COUNT(*) FROM major_towns t WHERE
            (:q IS NULL OR LOWER(t.town_name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR
            LOWER(t.district_name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))) AND
            (:stateName IS NULL OR t.state_name = CAST(:stateName AS text)) AND
            (:districtName IS NULL OR t.district_name = CAST(:districtName AS text))
            """,
           nativeQuery = true)
    Page<MajorTown> searchTowns(
            @Param("q") String q,
            @Param("stateName") String stateName,
            @Param("districtName") String districtName,
            Pageable pageable
    );

    // Get towns as GeoJSON for a state
    @Query(value = """
            SELECT json_build_object(
                'type', 'FeatureCollection',
                'features', COALESCE(json_agg(
                    json_build_object(
                        'type', 'Feature',
                        'geometry', ST_AsGeoJSON(location)::json,
                        'properties', json_build_object(
                            'id', id,
                            'townId', town_id,
                            'townName', town_name,
                            'districtName', district_name,
                            'stateName', state_name,
                            'elevation', elevation
                        )
                    )
                ), '[]'::json)
            )
            FROM major_towns
            WHERE state_name = :stateName
            """, nativeQuery = true)
    String findByStateAsGeoJson(@Param("stateName") String stateName);

    // Get towns as GeoJSON for a district
    @Query(value = """
            SELECT json_build_object(
                'type', 'FeatureCollection',
                'features', COALESCE(json_agg(
                    json_build_object(
                        'type', 'Feature',
                        'geometry', ST_AsGeoJSON(location)::json,
                        'properties', json_build_object(
                            'id', id,
                            'townId', town_id,
                            'townName', town_name,
                            'districtName', district_name,
                            'stateName', state_name,
                            'elevation', elevation
                        )
                    )
                ), '[]'::json)
            )
            FROM major_towns
            WHERE district_name = :districtName
            """, nativeQuery = true)
    String findByDistrictAsGeoJson(@Param("districtName") String districtName);
}
