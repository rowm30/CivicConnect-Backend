package com.civicconnect.api.repository;

import com.civicconnect.api.entity.WardCouncillor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Ward Councillor data access
 */
@Repository
public interface WardCouncillorRepository extends JpaRepository<WardCouncillor, Long> {

    /**
     * Find councillor by ward number and city
     */
    Optional<WardCouncillor> findByWardNoAndCityIgnoreCase(Integer wardNo, String city);

    /**
     * Find councillor by ward name and city
     */
    Optional<WardCouncillor> findByWardNameIgnoreCaseAndCityIgnoreCase(String wardName, String city);

    /**
     * Find all councillors for a city
     */
    List<WardCouncillor> findByCityIgnoreCaseOrderByWardNo(String city);

    /**
     * Find all councillors for a state
     */
    List<WardCouncillor> findByStateIgnoreCaseOrderByCityAscWardNoAsc(String state);

    /**
     * Find councillors by party affiliation
     */
    List<WardCouncillor> findByPartyAffiliationIgnoreCaseAndCityIgnoreCase(String party, String city);

    /**
     * Search councillors by ward name containing text
     */
    List<WardCouncillor> findByWardNameContainingIgnoreCaseAndCityIgnoreCase(String wardName, String city);

    /**
     * Search councillors by councillor name
     */
    List<WardCouncillor> findByCouncillorNameContainingIgnoreCase(String name);

    /**
     * Check if councillor exists for ward
     */
    boolean existsByWardNoAndCityIgnoreCase(Integer wardNo, String city);

    /**
     * Count councillors by city
     */
    long countByCityIgnoreCase(String city);

    /**
     * Count councillors by party in a city
     */
    long countByPartyAffiliationIgnoreCaseAndCityIgnoreCase(String party, String city);

    /**
     * Get party-wise count for a city
     */
    @Query("SELECT w.partyAffiliation, COUNT(w) FROM WardCouncillor w " +
           "WHERE LOWER(w.city) = LOWER(:city) " +
           "GROUP BY w.partyAffiliation ORDER BY COUNT(w) DESC")
    List<Object[]> getPartyWiseCountByCity(@Param("city") String city);

    /**
     * Find councillors by city and state (for multi-city states)
     */
    List<WardCouncillor> findByCityIgnoreCaseAndStateIgnoreCaseOrderByWardNo(String city, String state);

    /**
     * Get all distinct cities that have councillor data
     */
    @Query("SELECT DISTINCT w.city FROM WardCouncillor w ORDER BY w.city")
    List<String> findAllCitiesWithCouncillors();

    /**
     * Get all distinct states that have councillor data
     */
    @Query("SELECT DISTINCT w.state FROM WardCouncillor w ORDER BY w.state")
    List<String> findAllStatesWithCouncillors();

    /**
     * Find councillor where locality matches or is contained in ward name
     * This is used for GPS-based matching when we have a locality name
     */
    @Query("SELECT w FROM WardCouncillor w " +
           "WHERE LOWER(w.city) = LOWER(:city) " +
           "AND (LOWER(w.wardName) = LOWER(:locality) " +
           "     OR LOWER(w.wardName) LIKE LOWER(CONCAT('%', :locality, '%')) " +
           "     OR LOWER(:locality) LIKE LOWER(CONCAT('%', w.wardName, '%'))) " +
           "ORDER BY " +
           "CASE WHEN LOWER(w.wardName) = LOWER(:locality) THEN 0 ELSE 1 END, " +
           "LENGTH(w.wardName)")
    List<WardCouncillor> findByLocalityMatch(@Param("locality") String locality, @Param("city") String city);

    /**
     * Find councillor by matching any part of a full address
     * Searches multiple keywords against ward names
     */
    @Query("SELECT w FROM WardCouncillor w " +
           "WHERE LOWER(w.city) = LOWER(:city) " +
           "AND (LOWER(:address) LIKE LOWER(CONCAT('%', w.wardName, '%')))")
    List<WardCouncillor> findByAddressMatch(@Param("address") String address, @Param("city") String city);

    /**
     * Find councillor by location coordinates (placeholder - needs spatial query)
     * For now, returns empty as we don't have ward boundaries in this entity
     */
    @Query("SELECT w FROM WardCouncillor w WHERE 1=0")
    Optional<WardCouncillor> findByLocation(@Param("latitude") Double latitude, @Param("longitude") Double longitude);

    /**
     * Find all councillors by city name (case insensitive)
     */
    @Query("SELECT w FROM WardCouncillor w WHERE LOWER(w.city) = LOWER(:cityName) ORDER BY w.wardNo")
    List<WardCouncillor> findByCityNameIgnoreCase(@Param("cityName") String cityName);

    /**
     * Find councillor by city and state
     */
    @Query("SELECT w FROM WardCouncillor w WHERE LOWER(w.city) = LOWER(:cityName) AND LOWER(w.state) = LOWER(:stateName) ORDER BY w.wardNo")
    List<WardCouncillor> findByCityAndStateIgnoreCase(@Param("cityName") String cityName, @Param("stateName") String stateName);
}
