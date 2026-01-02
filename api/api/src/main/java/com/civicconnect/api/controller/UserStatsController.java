package com.civicconnect.api.controller;

import com.civicconnect.api.service.BenefitService;
import com.civicconnect.api.service.IssueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * User Stats Controller for Profile feature
 * Aggregates statistics from issues and benefits
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserStatsController {

    private final IssueService issueService;
    private final BenefitService benefitService;

    /**
     * Get comprehensive user stats for profile page
     */
    @GetMapping("/{userId}/stats")
    public ResponseEntity<UserProfileStats> getUserProfileStats(@PathVariable Long userId) {
        log.info("Getting profile stats for user: {}", userId);

        // Get issue stats
        IssueService.UserIssueStats issueStats = issueService.getUserStats(userId);

        // Get benefit stats
        BenefitService.UserBenefitStats benefitStats = benefitService.getUserBenefitStats(userId);

        UserProfileStats stats = new UserProfileStats(
                issueStats.issuesReported(),
                issueStats.issuesResolved(),
                issueStats.votesGiven(),
                benefitStats.savedCount(),
                benefitStats.appliedCount(),
                calculateCivicScore(issueStats, benefitStats)
        );

        return ResponseEntity.ok(stats);
    }

    /**
     * Get quick stats (for profile header)
     */
    @GetMapping("/{userId}/quick-stats")
    public ResponseEntity<QuickStats> getQuickStats(@PathVariable Long userId) {
        log.info("Getting quick stats for user: {}", userId);

        IssueService.UserIssueStats issueStats = issueService.getUserStats(userId);
        BenefitService.UserBenefitStats benefitStats = benefitService.getUserBenefitStats(userId);

        QuickStats stats = new QuickStats(
                issueStats.issuesReported(),
                issueStats.votesGiven(),
                benefitStats.savedCount() + benefitStats.appliedCount()
        );

        return ResponseEntity.ok(stats);
    }

    /**
     * Calculate civic score based on user activity
     * Score ranges from 0-100
     */
    private Integer calculateCivicScore(
            IssueService.UserIssueStats issueStats,
            BenefitService.UserBenefitStats benefitStats
    ) {
        int score = 0;

        // Issues reported (max 30 points)
        score += Math.min(issueStats.issuesReported() * 5, 30);

        // Issues resolved (max 20 points)
        score += Math.min(issueStats.issuesResolved() * 10, 20);

        // Votes given (max 25 points)
        score += Math.min(issueStats.votesGiven() * 2, 25);

        // Benefits explored (max 15 points)
        score += Math.min(benefitStats.savedCount() * 3, 15);

        // Benefits applied (max 10 points)
        score += Math.min(benefitStats.appliedCount() * 5, 10);

        return Math.min(score, 100);
    }

    // Response DTOs

    public record UserProfileStats(
            Long issuesReported,
            Long issuesResolved,
            Long votesGiven,
            Long benefitsSaved,
            Long benefitsApplied,
            Integer civicScore
    ) {}

    public record QuickStats(
            Long issuesReported,
            Long votesGiven,
            Long benefitsExplored
    ) {}
}
