package com.civicconnect.api.repository;

import com.civicconnect.api.entity.ChiefMinister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChiefMinisterRepository extends JpaRepository<ChiefMinister, Long> {

    /**
     * Find current CM by state code
     */
    Optional<ChiefMinister> findByStateCodeAndStatus(String stateCode, ChiefMinister.CMStatus status);

    /**
     * Find current CM by state name (case-insensitive)
     */
    @Query("SELECT c FROM ChiefMinister c WHERE LOWER(c.stateName) = LOWER(:stateName) AND c.status = :status")
    Optional<ChiefMinister> findByStateNameIgnoreCaseAndStatus(@Param("stateName") String stateName, @Param("status") ChiefMinister.CMStatus status);

    /**
     * Find all current CMs
     */
    List<ChiefMinister> findAllByStatus(ChiefMinister.CMStatus status);

    /**
     * Find all CMs (current and former) for a state
     */
    List<ChiefMinister> findByStateCodeOrderByTermStartDateDesc(String stateCode);

    /**
     * Find by state name
     */
    List<ChiefMinister> findByStateNameIgnoreCaseOrderByTermStartDateDesc(String stateName);

    /**
     * Search by name
     */
    @Query("SELECT c FROM ChiefMinister c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<ChiefMinister> searchByName(@Param("name") String name);

    /**
     * Get distinct state codes that have CM data
     */
    @Query("SELECT DISTINCT c.stateCode FROM ChiefMinister c WHERE c.status = 'CURRENT'")
    List<String> getStatesWithCMData();

    /**
     * Count CMs by party
     */
    @Query("SELECT c.partyAbbreviation, COUNT(c) FROM ChiefMinister c WHERE c.status = 'CURRENT' GROUP BY c.partyAbbreviation")
    List<Object[]> countByParty();
}
