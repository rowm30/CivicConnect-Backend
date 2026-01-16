package com.civicconnect.api.repository;

import com.civicconnect.api.entity.Mayor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MayorRepository extends JpaRepository<Mayor, Long> {

    /**
     * Find current Mayor by city name
     */
    @Query("SELECT m FROM Mayor m WHERE LOWER(m.cityName) = LOWER(:cityName) AND m.status = :status")
    Optional<Mayor> findByCityNameIgnoreCaseAndStatus(@Param("cityName") String cityName, @Param("status") Mayor.MayorStatus status);

    /**
     * Find current Mayor by city name and state
     */
    @Query("SELECT m FROM Mayor m WHERE LOWER(m.cityName) = LOWER(:cityName) AND LOWER(m.stateName) = LOWER(:stateName) AND m.status = :status")
    Optional<Mayor> findByCityAndStateAndStatus(
            @Param("cityName") String cityName,
            @Param("stateName") String stateName,
            @Param("status") Mayor.MayorStatus status);

    /**
     * Find all current mayors
     */
    List<Mayor> findAllByStatus(Mayor.MayorStatus status);

    /**
     * Find all mayors in a state
     */
    List<Mayor> findByStateNameIgnoreCaseAndStatus(String stateName, Mayor.MayorStatus status);

    /**
     * Find by state code
     */
    List<Mayor> findByStateCodeAndStatus(String stateCode, Mayor.MayorStatus status);

    /**
     * Search by city name or mayor name
     */
    @Query("SELECT m FROM Mayor m WHERE (LOWER(m.cityName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND m.status = 'CURRENT'")
    List<Mayor> searchByCityOrName(@Param("search") String search);

    /**
     * Get distinct cities that have mayor data
     */
    @Query("SELECT DISTINCT m.cityName FROM Mayor m WHERE m.status = 'CURRENT' ORDER BY m.cityName")
    List<String> getCitiesWithMayorData();

    /**
     * Get distinct states that have mayor data
     */
    @Query("SELECT DISTINCT m.stateName FROM Mayor m WHERE m.status = 'CURRENT' ORDER BY m.stateName")
    List<String> getStatesWithMayorData();

    /**
     * Count mayors by municipal body type
     */
    @Query("SELECT m.municipalBodyType, COUNT(m) FROM Mayor m WHERE m.status = 'CURRENT' GROUP BY m.municipalBodyType")
    List<Object[]> countByMunicipalBodyType();

    /**
     * Count mayors by state
     */
    @Query("SELECT m.stateName, COUNT(m) FROM Mayor m WHERE m.status = 'CURRENT' GROUP BY m.stateName ORDER BY COUNT(m) DESC")
    List<Object[]> countByState();
}
