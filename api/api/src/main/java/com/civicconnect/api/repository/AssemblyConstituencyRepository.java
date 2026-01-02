package com.civicconnect.api.repository;

import com.civicconnect.api.entity.AssemblyConstituency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssemblyConstituencyRepository extends JpaRepository<AssemblyConstituency, Long> {

    Optional<AssemblyConstituency> findByAcId(String acId);

    List<AssemblyConstituency> findByStateCode(String stateCode);

    List<AssemblyConstituency> findByStateName(String stateName);

    List<AssemblyConstituency> findByDistrictName(String districtName);

    List<AssemblyConstituency> findByPcId(String pcId);

    List<AssemblyConstituency> findByIsActiveTrue();

    List<AssemblyConstituency> findByStateCodeAndIsActiveTrue(String stateCode);

    List<AssemblyConstituency> findByAcNameContainingIgnoreCaseAndIsActiveTrue(String acName);

    @Query("SELECT ac FROM AssemblyConstituency ac WHERE " +
            "LOWER(ac.acName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(ac.stateName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(ac.districtName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(ac.acId) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<AssemblyConstituency> search(@Param("search") String search, Pageable pageable);

    // Find constituency containing a point (for GPS lookup)
    @Query(value = "SELECT * FROM assembly_constituencies WHERE ST_Contains(boundary, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)) LIMIT 1", nativeQuery = true)
    Optional<AssemblyConstituency> findByPoint(@Param("lat") double lat, @Param("lng") double lng);

    // Get all constituencies as GeoJSON FeatureCollection
    @Query(value = """
        SELECT json_build_object(
            'type', 'FeatureCollection',
            'features', COALESCE(json_agg(
                json_build_object(
                    'type', 'Feature',
                    'id', ac.id,
                    'geometry', ST_AsGeoJSON(ac.boundary)::json,
                    'properties', json_build_object(
                        'id', ac.id,
                        'acId', ac.ac_id,
                        'acName', ac.ac_name,
                        'acNo', ac.ac_no,
                        'stateCode', ac.state_code,
                        'stateName', ac.state_name,
                        'districtName', ac.district_name,
                        'pcName', ac.pc_name,
                        'reservedCategory', ac.reserved_category,
                        'currentMlaName', ac.current_mla_name,
                        'currentMlaParty', ac.current_mla_party,
                        'areaSqKm', ac.area_sq_km
                    )
                )
            ), '[]'::json)
        )::text
        FROM assembly_constituencies ac
        WHERE ac.is_active = true
        """, nativeQuery = true)
    String findAllAsGeoJson();

    // Get constituencies by state as GeoJSON
    @Query(value = """
        SELECT json_build_object(
            'type', 'FeatureCollection',
            'features', COALESCE(json_agg(
                json_build_object(
                    'type', 'Feature',
                    'id', ac.id,
                    'geometry', ST_AsGeoJSON(ac.boundary)::json,
                    'properties', json_build_object(
                        'id', ac.id,
                        'acId', ac.ac_id,
                        'acName', ac.ac_name,
                        'acNo', ac.ac_no,
                        'stateCode', ac.state_code,
                        'stateName', ac.state_name,
                        'districtName', ac.district_name,
                        'pcName', ac.pc_name,
                        'reservedCategory', ac.reserved_category,
                        'currentMlaName', ac.current_mla_name,
                        'currentMlaParty', ac.current_mla_party,
                        'areaSqKm', ac.area_sq_km
                    )
                )
            ), '[]'::json)
        )::text
        FROM assembly_constituencies ac
        WHERE ac.state_name = :stateName AND ac.is_active = true
        """, nativeQuery = true)
    String findByStateNameAsGeoJson(@Param("stateName") String stateName);

    // Get simplified geometry for better performance
    @Query(value = """
        SELECT json_build_object(
            'type', 'FeatureCollection',
            'features', COALESCE(json_agg(
                json_build_object(
                    'type', 'Feature',
                    'id', ac.id,
                    'geometry', ST_AsGeoJSON(ST_Simplify(ac.boundary, :tolerance))::json,
                    'properties', json_build_object(
                        'id', ac.id,
                        'acId', ac.ac_id,
                        'acName', ac.ac_name,
                        'stateCode', ac.state_code,
                        'stateName', ac.state_name,
                        'districtName', ac.district_name,
                        'reservedCategory', ac.reserved_category
                    )
                )
            ), '[]'::json)
        )::text
        FROM assembly_constituencies ac
        WHERE ac.is_active = true
        """, nativeQuery = true)
    String findAllAsSimplifiedGeoJson(@Param("tolerance") double tolerance);

    @Query("SELECT DISTINCT ac.stateName FROM AssemblyConstituency ac WHERE ac.isActive = true ORDER BY ac.stateName")
    List<String> findDistinctStateNames();

    @Query("SELECT DISTINCT ac.stateCode FROM AssemblyConstituency ac WHERE ac.isActive = true ORDER BY ac.stateCode")
    List<String> findDistinctStateCodes();

    @Query("SELECT DISTINCT ac.districtName FROM AssemblyConstituency ac WHERE ac.stateName = :stateName AND ac.isActive = true ORDER BY ac.districtName")
    List<String> findDistinctDistrictsByState(@Param("stateName") String stateName);

    @Query(value = "SELECT COUNT(*) FROM assembly_constituencies WHERE state_name = :stateName AND is_active = true", nativeQuery = true)
    long countByStateName(@Param("stateName") String stateName);

    // Find by AC number and state name (for MLA mapping) - case insensitive state name
    @Query("SELECT ac FROM AssemblyConstituency ac WHERE ac.acNo = :acNo AND UPPER(ac.stateName) = UPPER(:stateName) AND ac.isActive = true ORDER BY ac.id")
    List<AssemblyConstituency> findByAcNoAndStateName(@Param("acNo") Integer acNo, @Param("stateName") String stateName);

    // Find by AC name and state name (alternative matching) - case insensitive
    @Query("SELECT ac FROM AssemblyConstituency ac WHERE LOWER(ac.acName) = LOWER(:acName) AND UPPER(ac.stateName) = UPPER(:stateName) AND ac.isActive = true ORDER BY ac.id")
    List<AssemblyConstituency> findByAcNameAndStateName(@Param("acName") String acName, @Param("stateName") String stateName);

    // Fuzzy match by AC name (for cases where names don't match exactly) - case insensitive
    @Query("SELECT ac FROM AssemblyConstituency ac WHERE UPPER(ac.stateName) = UPPER(:stateName) AND ac.isActive = true AND " +
            "(LOWER(ac.acName) LIKE LOWER(CONCAT('%', :acName, '%')) OR LOWER(:acName) LIKE LOWER(CONCAT('%', ac.acName, '%')))")
    List<AssemblyConstituency> findByAcNameFuzzyAndStateName(@Param("acName") String acName, @Param("stateName") String stateName);
}
