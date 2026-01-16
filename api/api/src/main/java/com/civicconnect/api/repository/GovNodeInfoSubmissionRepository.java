package com.civicconnect.api.repository;

import com.civicconnect.api.entity.GovNodeInfoSubmission;
import com.civicconnect.api.entity.GovNodeInfoSubmission.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GovNodeInfoSubmissionRepository extends JpaRepository<GovNodeInfoSubmission, Long> {

    // Find submissions by status
    Page<GovNodeInfoSubmission> findByStatus(SubmissionStatus status, Pageable pageable);

    // Find submissions by user
    Page<GovNodeInfoSubmission> findBySubmittedById(Long userId, Pageable pageable);

    // Find submissions by node type
    Page<GovNodeInfoSubmission> findByNodeType(String nodeType, Pageable pageable);

    // Find submissions by state
    Page<GovNodeInfoSubmission> findByStateName(String stateName, Pageable pageable);

    // Find submissions by hierarchy mode
    Page<GovNodeInfoSubmission> findByHierarchyMode(String hierarchyMode, Pageable pageable);

    // Count pending submissions
    Long countByStatus(SubmissionStatus status);

    // Check for duplicate submissions (same node type, location, and official name)
    @Query("SELECT s FROM GovNodeInfoSubmission s WHERE s.nodeType = :nodeType " +
           "AND s.stateName = :stateName " +
           "AND LOWER(s.officialName) = LOWER(:officialName) " +
           "AND s.status != 'REJECTED'")
    List<GovNodeInfoSubmission> findPotentialDuplicates(
            @Param("nodeType") String nodeType,
            @Param("stateName") String stateName,
            @Param("officialName") String officialName
    );

    // Find pending submissions for a specific location and node type
    @Query("SELECT s FROM GovNodeInfoSubmission s WHERE s.nodeType = :nodeType " +
           "AND s.hierarchyMode = :mode " +
           "AND (:stateName IS NULL OR s.stateName = :stateName) " +
           "AND s.status = 'PENDING'")
    List<GovNodeInfoSubmission> findPendingForNode(
            @Param("nodeType") String nodeType,
            @Param("mode") String mode,
            @Param("stateName") String stateName
    );
}
