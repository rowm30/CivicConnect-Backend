package com.civicconnect.api.repository;

import com.civicconnect.api.entity.MemberOfParliament;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberOfParliamentRepository extends JpaRepository<MemberOfParliament, Long> {

    List<MemberOfParliament> findByStateName(String stateName);

    List<MemberOfParliament> findByPartyName(String partyName);

    Optional<MemberOfParliament> findByConstituencyNameAndStateName(String constituencyName, String stateName);

    Page<MemberOfParliament> findByStateName(String stateName, Pageable pageable);

    Page<MemberOfParliament> findByPartyName(String partyName, Pageable pageable);

    @Query("SELECT DISTINCT m.stateName FROM MemberOfParliament m WHERE m.isActive = true ORDER BY m.stateName")
    List<String> findDistinctStateNames();

    @Query("SELECT DISTINCT m.partyName FROM MemberOfParliament m WHERE m.isActive = true ORDER BY m.partyName")
    List<String> findDistinctPartyNames();

    @Query(value = """
            SELECT * FROM members_of_parliament m WHERE m.is_active = true AND
            (:q IS NULL OR LOWER(m.member_name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR
            LOWER(m.constituency_name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR
            LOWER(m.party_name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))) AND
            (:stateName IS NULL OR m.state_name = CAST(:stateName AS text)) AND
            (:partyName IS NULL OR m.party_name = CAST(:partyName AS text))
            """,
           countQuery = """
            SELECT COUNT(*) FROM members_of_parliament m WHERE m.is_active = true AND
            (:q IS NULL OR LOWER(m.member_name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR
            LOWER(m.constituency_name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR
            LOWER(m.party_name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))) AND
            (:stateName IS NULL OR m.state_name = CAST(:stateName AS text)) AND
            (:partyName IS NULL OR m.party_name = CAST(:partyName AS text))
            """,
           nativeQuery = true)
    Page<MemberOfParliament> searchMembers(
            @Param("q") String q,
            @Param("stateName") String stateName,
            @Param("partyName") String partyName,
            Pageable pageable
    );

    // Count MPs by party
    @Query("SELECT m.partyName, COUNT(m) FROM MemberOfParliament m WHERE m.isActive = true GROUP BY m.partyName ORDER BY COUNT(m) DESC")
    List<Object[]> countByParty();

    // Count MPs by state
    @Query("SELECT m.stateName, COUNT(m) FROM MemberOfParliament m WHERE m.isActive = true GROUP BY m.stateName ORDER BY m.stateName")
    List<Object[]> countByState();

    // Find MP by constituency ID
    Optional<MemberOfParliament> findByConstituencyIdAndIsActiveTrue(Long constituencyId);

    // Find all active MPs
    List<MemberOfParliament> findByIsActiveTrue();

    // Find by constituency name (case insensitive, partial match)
    List<MemberOfParliament> findByConstituencyNameContainingIgnoreCaseAndIsActiveTrue(String constituencyName);

    // Find by Parliamentary Constituency ID (for location-based lookup)
    @Query("SELECT m FROM MemberOfParliament m WHERE m.constituency.id = :constituencyId AND m.isActive = true")
    Optional<MemberOfParliament> findByConstituencyIdActive(@Param("constituencyId") Long constituencyId);

    // Find by constituency name and state (for fallback lookup)
    @Query("SELECT m FROM MemberOfParliament m WHERE UPPER(m.constituencyName) = UPPER(:pcName) AND UPPER(m.stateName) = UPPER(:stateName) AND m.isActive = true")
    Optional<MemberOfParliament> findByPcNameAndStateName(@Param("pcName") String pcName, @Param("stateName") String stateName);
}
