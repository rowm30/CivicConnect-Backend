package com.civicconnect.api.repository;

import com.civicconnect.api.entity.StateBoundary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StateBoundaryRepository extends JpaRepository<StateBoundary, Long> {

    Optional<StateBoundary> findByStateId(String stateId);

    Optional<StateBoundary> findByStateName(String stateName);

    @Query("SELECT s.stateName FROM StateBoundary s ORDER BY s.stateName")
    List<String> findAllStateNames();

    // Find state by GPS coordinates
    @Query(value = """
            SELECT * FROM state_boundaries
            WHERE ST_Contains(boundary, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326))
            LIMIT 1
            """, nativeQuery = true)
    Optional<StateBoundary> findByPoint(@Param("lat") double lat, @Param("lng") double lng);

    // Get all states as GeoJSON
    @Query(value = """
            SELECT json_build_object(
                'type', 'FeatureCollection',
                'features', COALESCE(json_agg(
                    json_build_object(
                        'type', 'Feature',
                        'geometry', ST_AsGeoJSON(boundary)::json,
                        'properties', json_build_object(
                            'id', id,
                            'stateId', state_id,
                            'stateName', state_name,
                            'areaSqKm', area_sq_km
                        )
                    )
                ), '[]'::json)
            )
            FROM state_boundaries
            """, nativeQuery = true)
    String findAllAsGeoJson();

    // Get simplified GeoJSON
    @Query(value = """
            SELECT json_build_object(
                'type', 'FeatureCollection',
                'features', COALESCE(json_agg(
                    json_build_object(
                        'type', 'Feature',
                        'geometry', ST_AsGeoJSON(ST_Simplify(boundary, :tolerance))::json,
                        'properties', json_build_object(
                            'id', id,
                            'stateId', state_id,
                            'stateName', state_name,
                            'areaSqKm', area_sq_km
                        )
                    )
                ), '[]'::json)
            )
            FROM state_boundaries
            """, nativeQuery = true)
    String findAllAsSimplifiedGeoJson(@Param("tolerance") double tolerance);

    // Get single state as GeoJSON
    @Query(value = """
            SELECT json_build_object(
                'type', 'FeatureCollection',
                'features', COALESCE(json_agg(
                    json_build_object(
                        'type', 'Feature',
                        'geometry', ST_AsGeoJSON(boundary)::json,
                        'properties', json_build_object(
                            'id', id,
                            'stateId', state_id,
                            'stateName', state_name,
                            'areaSqKm', area_sq_km
                        )
                    )
                ), '[]'::json)
            )
            FROM state_boundaries
            WHERE state_name = :stateName
            """, nativeQuery = true)
    String findByStateNameAsGeoJson(@Param("stateName") String stateName);
}
