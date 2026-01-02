package com.civicconnect.api.repository;

import com.civicconnect.api.entity.IssueVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IssueVoteRepository extends JpaRepository<IssueVote, Long> {

    // Find existing vote by user on issue
    @Query("SELECT v FROM IssueVote v WHERE v.issue.id = :issueId AND v.user.id = :userId")
    Optional<IssueVote> findByIssueIdAndUserId(
            @Param("issueId") Long issueId,
            @Param("userId") Long userId
    );

    // Count upvotes for an issue
    @Query("SELECT COUNT(v) FROM IssueVote v WHERE v.issue.id = :issueId AND v.voteType = 'UPVOTE'")
    Long countUpvotes(@Param("issueId") Long issueId);

    // Count downvotes for an issue
    @Query("SELECT COUNT(v) FROM IssueVote v WHERE v.issue.id = :issueId AND v.voteType = 'DOWNVOTE'")
    Long countDownvotes(@Param("issueId") Long issueId);

    // Count total votes by user
    @Query("SELECT COUNT(v) FROM IssueVote v WHERE v.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    // Check if user has voted on issue
    boolean existsByIssueIdAndUserId(Long issueId, Long userId);

    // Delete vote by user on issue
    void deleteByIssueIdAndUserId(Long issueId, Long userId);
}
