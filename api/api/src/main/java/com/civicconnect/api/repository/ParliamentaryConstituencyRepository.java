package com.civicconnect.api.repository;

import com.civicconnect.api.entity.ParliamentaryConstituency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParliamentaryConstituencyRepository extends JpaRepository<ParliamentaryConstituency, Long> {

    Optional<ParliamentaryConstituency> findByPcId(String pcId);

    List<ParliamentaryConstituency> findByStateCode(String stateCode);

    List<ParliamentaryConstituency> findByStateName(String stateName);

    List<ParliamentaryConstituency> findByIsActiveTrue();

    List<ParliamentaryConstituency> findByStateCodeAndIsActiveTrue(String stateCode);

    @Query("SELECT pc FROM ParliamentaryConstituency pc WHERE " +
            "LOWER(pc.pcName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(pc.stateName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(pc.pcId) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<ParliamentaryConstituency> search(@Param("search") String search, Pageable pageable);

    // Find constituency containing a point (for GPS lookup)
    @Query(value = "SELECT * FROM parliamentary_constituencies WHERE ST_Contains(boundary, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326))", nativeQuery = true)
    Optional<ParliamentaryConstituency> findByPoint(@Param("lat") double lat, @Param("lng") double lng);

    // Get all constituencies as GeoJSON FeatureCollection with MP details
    @Query(value = """
        SELECT json_build_object(
            'type', 'FeatureCollection',
            'features', COALESCE(json_agg(
                json_build_object(
                    'type', 'Feature',
                    'id', pc.id,
                    'geometry', ST_AsGeoJSON(pc.boundary)::json,
                    'properties', json_build_object(
                        'id', pc.id,
                        'pcId', pc.pc_id,
                        'pcName', pc.pc_name,
                        'pcNameHi', pc.pc_name_hi,
                        'stateCode', pc.state_code,
                        'stateName', pc.state_name,
                        'reservedCategory', pc.reserved_category,
                        'currentMpName', COALESCE(mp.member_name, pc.current_mp_name),
                        'currentMpParty', COALESCE(mp.party_name, pc.current_mp_party),
                        'mpPartyAbbr', mp.party_abbreviation,
                        'mpTerms', mp.lok_sabha_terms,
                        'areaSqKm', pc.area_sq_km
                    )
                )
            ), '[]'::json)
        )::text
        FROM parliamentary_constituencies pc
        LEFT JOIN members_of_parliament mp ON mp.constituency_id = pc.id AND mp.is_active = true
        WHERE pc.is_active = true
        """, nativeQuery = true)
    String findAllAsGeoJson();

    // Get constituencies by state as GeoJSON with MP details
    @Query(value = """
        SELECT json_build_object(
            'type', 'FeatureCollection',
            'features', COALESCE(json_agg(
                json_build_object(
                    'type', 'Feature',
                    'id', pc.id,
                    'geometry', ST_AsGeoJSON(pc.boundary)::json,
                    'properties', json_build_object(
                        'id', pc.id,
                        'pcId', pc.pc_id,
                        'pcName', pc.pc_name,
                        'pcNameHi', pc.pc_name_hi,
                        'stateCode', pc.state_code,
                        'stateName', pc.state_name,
                        'reservedCategory', pc.reserved_category,
                        'currentMpName', COALESCE(mp.member_name, pc.current_mp_name),
                        'currentMpParty', COALESCE(mp.party_name, pc.current_mp_party),
                        'mpPartyAbbr', mp.party_abbreviation,
                        'mpTerms', mp.lok_sabha_terms,
                        'areaSqKm', pc.area_sq_km
                    )
                )
            ), '[]'::json)
        )::text
        FROM parliamentary_constituencies pc
        LEFT JOIN members_of_parliament mp ON mp.constituency_id = pc.id AND mp.is_active = true
        WHERE pc.state_name = :stateName AND pc.is_active = true
        """, nativeQuery = true)
    String findByStateNameAsGeoJson(@Param("stateName") String stateName);

    // Get simplified geometry for better performance - includes MP data
    @Query(value = """
        SELECT json_build_object(
            'type', 'FeatureCollection',
            'features', COALESCE(json_agg(
                json_build_object(
                    'type', 'Feature',
                    'id', pc.id,
                    'geometry', ST_AsGeoJSON(ST_Simplify(pc.boundary, :tolerance))::json,
                    'properties', json_build_object(
                        'id', pc.id,
                        'pcId', pc.pc_id,
                        'pcName', pc.pc_name,
                        'stateCode', pc.state_code,
                        'stateName', pc.state_name,
                        'reservedCategory', pc.reserved_category,
                        'currentMpName', COALESCE(mp.member_name, pc.current_mp_name),
                        'currentMpParty', COALESCE(mp.party_name, pc.current_mp_party),
                        'mpPartyAbbr', mp.party_abbreviation,
                        'mpTerms', mp.lok_sabha_terms
                    )
                )
            ), '[]'::json)
        )::text
        FROM parliamentary_constituencies pc
        LEFT JOIN members_of_parliament mp ON mp.constituency_id = pc.id AND mp.is_active = true
        WHERE pc.is_active = true
        """, nativeQuery = true)
    String findAllAsSimplifiedGeoJson(@Param("tolerance") double tolerance);

    boolean existsByPcId(String pcId);

    @Query("SELECT DISTINCT pc.stateName FROM ParliamentaryConstituency pc WHERE pc.isActive = true ORDER BY pc.stateName")
    List<String> findDistinctStateNames();

    @Query("SELECT DISTINCT pc.stateCode FROM ParliamentaryConstituency pc WHERE pc.isActive = true ORDER BY pc.stateCode")
    List<String> findDistinctStateCodes();
}
