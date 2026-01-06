package com.civicconnect.api.repository;

import com.civicconnect.api.entity.Issue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    // Find active issues ordered by heat score (upvotes ratio)
    @Query("SELECT i FROM Issue i WHERE i.isActive = true ORDER BY " +
           "(CAST(i.upvoteCount AS float) / (i.upvoteCount + i.downvoteCount + 1)) DESC")
    Page<Issue> findHottestIssues(Pageable pageable);

    // Find active issues by category
    @Query("SELECT i FROM Issue i WHERE i.isActive = true AND i.category = :category " +
           "ORDER BY (CAST(i.upvoteCount AS float) / (i.upvoteCount + i.downvoteCount + 1)) DESC")
    Page<Issue> findByCategoryOrderByHeatScore(
            @Param("category") Issue.IssueCategory category,
            Pageable pageable
    );

    // Find active issues by status
    @Query("SELECT i FROM Issue i WHERE i.isActive = true AND i.status = :status " +
           "ORDER BY (CAST(i.upvoteCount AS float) / (i.upvoteCount + i.downvoteCount + 1)) DESC")
    Page<Issue> findByStatusOrderByHeatScore(
            @Param("status") Issue.IssueStatus status,
            Pageable pageable
    );

    // Find issues near a location
    @Query("SELECT i FROM Issue i WHERE i.isActive = true " +
           "AND i.latitude BETWEEN :minLat AND :maxLat " +
           "AND i.longitude BETWEEN :minLng AND :maxLng " +
           "ORDER BY (CAST(i.upvoteCount AS float) / (i.upvoteCount + i.downvoteCount + 1)) DESC")
    Page<Issue> findNearbyIssues(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng,
            Pageable pageable
    );

    // Find by tracking ID
    Optional<Issue> findByTrackingId(String trackingId);

    // Find issues by reporter
    @Query("SELECT i FROM Issue i WHERE i.reporter.id = :userId ORDER BY i.createdAt DESC")
    Page<Issue> findByReporterId(@Param("userId") Long userId, Pageable pageable);

    // Count issues by reporter
    @Query("SELECT COUNT(i) FROM Issue i WHERE i.reporter.id = :userId")
    Long countByReporterId(@Param("userId") Long userId);

    // Count resolved issues by reporter
    @Query("SELECT COUNT(i) FROM Issue i WHERE i.reporter.id = :userId AND i.status = 'RESOLVED'")
    Long countResolvedByReporterId(@Param("userId") Long userId);

    // Find issues by state
    @Query("SELECT i FROM Issue i WHERE i.isActive = true AND i.stateName = :stateName " +
           "ORDER BY (CAST(i.upvoteCount AS float) / (i.upvoteCount + i.downvoteCount + 1)) DESC")
    Page<Issue> findByStateName(@Param("stateName") String stateName, Pageable pageable);

    // Find issues by district
    @Query("SELECT i FROM Issue i WHERE i.isActive = true AND i.districtName = :districtName " +
           "ORDER BY (CAST(i.upvoteCount AS float) / (i.upvoteCount + i.downvoteCount + 1)) DESC")
    Page<Issue> findByDistrictName(@Param("districtName") String districtName, Pageable pageable);

    // Search issues by title or description
    @Query("SELECT i FROM Issue i WHERE i.isActive = true " +
           "AND (LOWER(i.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY (CAST(i.upvoteCount AS float) / (i.upvoteCount + i.downvoteCount + 1)) DESC")
    Page<Issue> searchIssues(@Param("query") String query, Pageable pageable);

    // Find issues by parliamentary constituency (primary filter for Issue Pulse)
    @Query("SELECT i FROM Issue i WHERE i.isActive = true AND i.parliamentaryConstituency = :constituency " +
           "ORDER BY (CAST(i.upvoteCount AS float) / (i.upvoteCount + i.downvoteCount + 1)) DESC")
    Page<Issue> findByParliamentaryConstituencyOrderByHeatScore(
            @Param("constituency") String constituency,
            Pageable pageable
    );

    // Find issues by assembly constituency
    @Query("SELECT i FROM Issue i WHERE i.isActive = true AND i.assemblyConstituency = :constituency " +
           "ORDER BY (CAST(i.upvoteCount AS float) / (i.upvoteCount + i.downvoteCount + 1)) DESC")
    Page<Issue> findByAssemblyConstituencyOrderByHeatScore(
            @Param("constituency") String constituency,
            Pageable pageable
    );

    // Find issues by either parliamentary or assembly constituency
    @Query("SELECT i FROM Issue i WHERE i.isActive = true " +
           "AND (i.parliamentaryConstituency = :pc OR i.assemblyConstituency = :ac) " +
           "ORDER BY (CAST(i.upvoteCount AS float) / (i.upvoteCount + i.downvoteCount + 1)) DESC")
    Page<Issue> findByConstituencyOrderByHeatScore(
            @Param("pc") String parliamentaryConstituency,
            @Param("ac") String assemblyConstituency,
            Pageable pageable
    );

    // ========== Issue counts by representative for Gov Map stats ==========

    // Count by Councillor ID
    @Query("SELECT COUNT(i) FROM Issue i WHERE i.councillorId = :councillorId")
    Long countByCouncillorId(@Param("councillorId") Long councillorId);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.councillorId = :councillorId AND i.status = com.civicconnect.api.entity.Issue$IssueStatus.RESOLVED")
    Long countByCouncillorIdResolved(@Param("councillorId") Long councillorId);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.councillorId = :councillorId AND i.status = com.civicconnect.api.entity.Issue$IssueStatus.PENDING")
    Long countByCouncillorIdPending(@Param("councillorId") Long councillorId);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.councillorId = :councillorId AND i.status = com.civicconnect.api.entity.Issue$IssueStatus.IN_PROGRESS")
    Long countByCouncillorIdInProgress(@Param("councillorId") Long councillorId);

    // Count by MLA ID
    @Query("SELECT COUNT(i) FROM Issue i WHERE i.mlaId = :mlaId")
    Long countByMlaId(@Param("mlaId") Long mlaId);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.mlaId = :mlaId AND i.status = com.civicconnect.api.entity.Issue$IssueStatus.RESOLVED")
    Long countByMlaIdResolved(@Param("mlaId") Long mlaId);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.mlaId = :mlaId AND i.status = com.civicconnect.api.entity.Issue$IssueStatus.PENDING")
    Long countByMlaIdPending(@Param("mlaId") Long mlaId);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.mlaId = :mlaId AND i.status = com.civicconnect.api.entity.Issue$IssueStatus.IN_PROGRESS")
    Long countByMlaIdInProgress(@Param("mlaId") Long mlaId);

    // Count by MP ID
    @Query("SELECT COUNT(i) FROM Issue i WHERE i.mpId = :mpId")
    Long countByMpId(@Param("mpId") Long mpId);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.mpId = :mpId AND i.status = com.civicconnect.api.entity.Issue$IssueStatus.RESOLVED")
    Long countByMpIdResolved(@Param("mpId") Long mpId);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.mpId = :mpId AND i.status = com.civicconnect.api.entity.Issue$IssueStatus.PENDING")
    Long countByMpIdPending(@Param("mpId") Long mpId);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.mpId = :mpId AND i.status = com.civicconnect.api.entity.Issue$IssueStatus.IN_PROGRESS")
    Long countByMpIdInProgress(@Param("mpId") Long mpId);

    // Count by Assembly Constituency (for MLA aggregate stats)
    @Query("SELECT COUNT(i) FROM Issue i WHERE i.assemblyConstituency = :ac")
    Long countByAssemblyConstituency(@Param("ac") String assemblyConstituency);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.assemblyConstituency = :ac AND i.status = :status")
    Long countByAssemblyConstituencyAndStatus(@Param("ac") String assemblyConstituency, @Param("status") String status);

    // Count by Parliamentary Constituency (for MP aggregate stats)
    @Query("SELECT COUNT(i) FROM Issue i WHERE i.parliamentaryConstituency = :pc")
    Long countByParliamentaryConstituency(@Param("pc") String parliamentaryConstituency);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.parliamentaryConstituency = :pc AND i.status = :status")
    Long countByParliamentaryConstituencyAndStatus(@Param("pc") String parliamentaryConstituency, @Param("status") String status);

    // Count by State (for state-level aggregate stats)
    @Query("SELECT COUNT(i) FROM Issue i WHERE i.stateName = :stateName")
    Long countByStateName(@Param("stateName") String stateName);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.stateName = :stateName AND i.status = :status")
    Long countByStateNameAndStatus(@Param("stateName") String stateName, @Param("status") String status);
}
