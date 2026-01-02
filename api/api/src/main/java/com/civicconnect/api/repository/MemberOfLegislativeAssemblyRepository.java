package com.civicconnect.api.repository;

import com.civicconnect.api.entity.MemberOfLegislativeAssembly;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberOfLegislativeAssemblyRepository extends JpaRepository<MemberOfLegislativeAssembly, Long> {

    // Find by state
    List<MemberOfLegislativeAssembly> findByStateName(String stateName);

    Page<MemberOfLegislativeAssembly> findByStateName(String stateName, Pageable pageable);

    // Find by state and party
    @Query("SELECT m FROM MemberOfLegislativeAssembly m WHERE m.stateName = :stateName AND (m.partyName LIKE %:partyName% OR m.partyAbbreviation LIKE %:partyName%)")
    Page<MemberOfLegislativeAssembly> findByStateNameAndPartyNameContaining(@Param("stateName") String stateName, @Param("partyName") String partyName, Pageable pageable);

    // Find by party
    @Query("SELECT m FROM MemberOfLegislativeAssembly m WHERE m.partyName LIKE %:partyName% OR m.partyAbbreviation LIKE %:partyName%")
    Page<MemberOfLegislativeAssembly> findByPartyNameContaining(@Param("partyName") String partyName, Pageable pageable);

    // Search by name with pagination
    @Query("SELECT m FROM MemberOfLegislativeAssembly m WHERE LOWER(m.memberName) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<MemberOfLegislativeAssembly> searchByNamePaged(@Param("query") String query, Pageable pageable);

    // Search by name within a state
    @Query("SELECT m FROM MemberOfLegislativeAssembly m WHERE LOWER(m.memberName) LIKE LOWER(CONCAT('%', :query, '%')) AND m.stateName = :stateName")
    Page<MemberOfLegislativeAssembly> searchByNameInState(@Param("query") String query, @Param("stateName") String stateName, Pageable pageable);

    // Distinct state names
    @Query("SELECT DISTINCT m.stateName FROM MemberOfLegislativeAssembly m WHERE m.stateName IS NOT NULL ORDER BY m.stateName")
    List<String> findDistinctStateNames();

    // Distinct party names
    @Query("SELECT DISTINCT m.partyName FROM MemberOfLegislativeAssembly m WHERE m.partyName IS NOT NULL ORDER BY m.partyName")
    List<String> findDistinctPartyNames();

    // Find by constituency
    Optional<MemberOfLegislativeAssembly> findByConstituencyNameAndStateName(String constituencyName, String stateName);

    List<MemberOfLegislativeAssembly> findByConstituencyNameContainingIgnoreCase(String query);

    // Find by AC number and state
    Optional<MemberOfLegislativeAssembly> findByAcNoAndStateName(Integer acNo, String stateName);

    // Find by party
    List<MemberOfLegislativeAssembly> findByPartyName(String partyName);

    List<MemberOfLegislativeAssembly> findByPartyNameAndStateName(String partyName, String stateName);

    // Find sitting MLAs
    @Query("SELECT m FROM MemberOfLegislativeAssembly m WHERE m.membershipStatus = 'Sitting' AND m.stateName = :stateName")
    List<MemberOfLegislativeAssembly> findSittingMlasByState(@Param("stateName") String stateName);

    // Count by state
    Long countByStateName(String stateName);

    // Count by party in a state
    @Query("SELECT m.partyName, COUNT(m) FROM MemberOfLegislativeAssembly m WHERE m.stateName = :stateName GROUP BY m.partyName ORDER BY COUNT(m) DESC")
    List<Object[]> countByPartyInState(@Param("stateName") String stateName);

    // Find by election year
    List<MemberOfLegislativeAssembly> findByElectionYearAndStateName(Integer electionYear, String stateName);

    // Search by name
    @Query("SELECT m FROM MemberOfLegislativeAssembly m WHERE LOWER(m.memberName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<MemberOfLegislativeAssembly> searchByName(@Param("query") String query);

    // Find by data source
    List<MemberOfLegislativeAssembly> findByDataSource(String dataSource);

    // Find with criminal cases
    @Query("SELECT m FROM MemberOfLegislativeAssembly m WHERE m.stateName = :stateName AND m.criminalCases > 0 ORDER BY m.criminalCases DESC")
    List<MemberOfLegislativeAssembly> findMlasWithCriminalCases(@Param("stateName") String stateName);

    // Find by source candidate ID (for update matching)
    Optional<MemberOfLegislativeAssembly> findBySourceCandidateIdAndDataSource(String sourceCandidateId, String dataSource);

    // Statistics
    @Query("SELECT m.stateName, COUNT(m) FROM MemberOfLegislativeAssembly m GROUP BY m.stateName ORDER BY COUNT(m) DESC")
    List<Object[]> countByState();

    @Query("SELECT AVG(m.age) FROM MemberOfLegislativeAssembly m WHERE m.stateName = :stateName")
    Double getAverageAge(@Param("stateName") String stateName);

    @Query("SELECT m.education, COUNT(m) FROM MemberOfLegislativeAssembly m WHERE m.stateName = :stateName GROUP BY m.education")
    List<Object[]> countByEducation(@Param("stateName") String stateName);

    // Find all for a state with pagination
    @Query("SELECT m FROM MemberOfLegislativeAssembly m WHERE m.stateName = :stateName ORDER BY m.acNo")
    Page<MemberOfLegislativeAssembly> findByStateNameOrderByAcNo(@Param("stateName") String stateName, Pageable pageable);

    // Delete all for a state (for refresh)
    void deleteByStateName(String stateName);

    // Find by Assembly Constituency ID
    Optional<MemberOfLegislativeAssembly> findByAssemblyConstituencyId(Long assemblyConstituencyId);
}
