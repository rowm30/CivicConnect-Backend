package com.civicconnect.api.repository;

import com.civicconnect.api.entity.StateCapital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StateCapitalRepository extends JpaRepository<StateCapital, Long> {

    Optional<StateCapital> findByCapitalId(String capitalId);

    Optional<StateCapital> findByStateName(String stateName);

    // Get all capitals as GeoJSON
    @Query(value = """
            SELECT json_build_object(
                'type', 'FeatureCollection',
                'features', COALESCE(json_agg(
                    json_build_object(
                        'type', 'Feature',
                        'geometry', ST_AsGeoJSON(location)::json,
                        'properties', json_build_object(
                            'id', id,
                            'capitalId', capital_id,
                            'stateName', state_name,
                            'capitalName', capital_name,
                            'elevation', elevation
                        )
                    )
                ), '[]'::json)
            )
            FROM state_capitals
            """, nativeQuery = true)
    String findAllAsGeoJson();
}
