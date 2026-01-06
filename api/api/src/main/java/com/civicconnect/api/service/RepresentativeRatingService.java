package com.civicconnect.api.service;

import com.civicconnect.api.dto.RepresentativeRatingDTO.*;
import com.civicconnect.api.entity.RepresentativeRating;
import com.civicconnect.api.entity.RepresentativeRating.RepresentativeType;
import com.civicconnect.api.entity.analytics.AppUser;
import com.civicconnect.api.repository.IssueRepository;
import com.civicconnect.api.repository.RepresentativeRatingRepository;
import com.civicconnect.api.repository.analytics.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing representative ratings and stats
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RepresentativeRatingService {

    private final RepresentativeRatingRepository ratingRepository;
    private final AppUserRepository userRepository;
    private final IssueRepository issueRepository;

    /**
     * Submit or update a rating
     */
    @Transactional
    public RatingResponse submitRating(Long userId, RatingRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Validate rating
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Check if user already rated this representative
        Optional<RepresentativeRating> existingRating = ratingRepository
                .findByUserIdAndRepresentativeTypeAndRepresentativeId(
                        userId, request.getRepresentativeType(), request.getRepresentativeId());

        RepresentativeRating rating;
        if (existingRating.isPresent()) {
            // Update existing rating
            rating = existingRating.get();
            rating.setRating(request.getRating());
            rating.setComment(request.getComment());
            if (request.getAspectRatings() != null) {
                rating.setAspectRatings(convertAspectRatingsToString(request.getAspectRatings()));
            }
            rating.setIsAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : false);
            log.info("Updated rating for user {} on {} {}", userId, request.getRepresentativeType(), request.getRepresentativeId());
        } else {
            // Create new rating
            rating = RepresentativeRating.builder()
                    .user(user)
                    .representativeType(request.getRepresentativeType())
                    .representativeId(request.getRepresentativeId())
                    .representativeName(request.getRepresentativeName())
                    .partyName(request.getPartyName())
                    .constituencyName(request.getConstituencyName())
                    .rating(request.getRating())
                    .comment(request.getComment())
                    .aspectRatings(request.getAspectRatings() != null ?
                            convertAspectRatingsToString(request.getAspectRatings()) : null)
                    .isAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : false)
                    .isVerified(false) // TODO: Verify based on user's constituency
                    .build();
            log.info("Created new rating for user {} on {} {}", userId, request.getRepresentativeType(), request.getRepresentativeId());
        }

        rating = ratingRepository.save(rating);
        return RatingResponse.fromEntity(rating);
    }

    /**
     * Get user's rating for a representative
     */
    public Optional<RatingResponse> getUserRating(Long userId, RepresentativeType type, Long representativeId) {
        return ratingRepository.findByUserIdAndRepresentativeTypeAndRepresentativeId(userId, type, representativeId)
                .map(RatingResponse::fromEntity);
    }

    /**
     * Get all ratings by a user
     */
    public List<RatingResponse> getUserRatings(Long userId) {
        return ratingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(RatingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get aggregate rating stats for a representative
     */
    public RatingStats getRatingStats(RepresentativeType type, Long representativeId) {
        Object[] stats = ratingRepository.getAggregateStats(type, representativeId);

        Double avgRating = stats[0] != null ? ((Number) stats[0]).doubleValue() : 0.0;
        Long totalCount = stats[1] != null ? ((Number) stats[1]).longValue() : 0L;
        Long fiveStar = stats[2] != null ? ((Number) stats[2]).longValue() : 0L;
        Long fourStar = stats[3] != null ? ((Number) stats[3]).longValue() : 0L;
        Long threeStar = stats[4] != null ? ((Number) stats[4]).longValue() : 0L;
        Long twoStar = stats[5] != null ? ((Number) stats[5]).longValue() : 0L;
        Long oneStar = stats[6] != null ? ((Number) stats[6]).longValue() : 0L;

        Long verifiedCount = ratingRepository.getVerifiedRatingCount(type, representativeId);

        return RatingStats.builder()
                .representativeType(type)
                .representativeId(representativeId)
                .averageRating(Math.round(avgRating * 10.0) / 10.0) // Round to 1 decimal
                .totalRatings(totalCount)
                .verifiedRatings(verifiedCount)
                .fiveStarCount(fiveStar)
                .fourStarCount(fourStar)
                .threeStarCount(threeStar)
                .twoStarCount(twoStar)
                .oneStarCount(oneStar)
                .approvalLevel(RatingStats.calculateApprovalLevel(avgRating))
                .build();
    }

    /**
     * Get combined representative stats for Gov Map
     */
    public RepresentativeStats getRepresentativeStats(RepresentativeType type, Long representativeId,
                                                       String name, String party, String constituency,
                                                       String photoUrl, String designation, Long userId) {
        // Get rating stats
        RatingStats ratingStats = getRatingStats(type, representativeId);

        // Get issue stats
        IssueStats issueStats = getIssueStats(type, representativeId, name);

        // Check if user has rated
        Integer userRating = null;
        boolean hasUserRated = false;
        if (userId != null) {
            Optional<RatingResponse> userRatingOpt = getUserRating(userId, type, representativeId);
            if (userRatingOpt.isPresent()) {
                userRating = userRatingOpt.get().getRating();
                hasUserRated = true;
            }
        }

        // Calculate accountability score (0-100)
        int accountabilityScore = calculateAccountabilityScore(ratingStats, issueStats);

        return RepresentativeStats.builder()
                .representativeType(type)
                .representativeId(representativeId)
                .name(name)
                .party(party)
                .constituency(constituency)
                .photoUrl(photoUrl)
                .designation(designation)
                .approvalRating(ratingStats.getAverageRating())
                .totalRatings(ratingStats.getTotalRatings())
                .approvalLevel(ratingStats.getApprovalLevel())
                .issuesRegistered(issueStats.getTotalIssues())
                .issuesResolved(issueStats.getResolvedIssues())
                .resolutionRate(issueStats.getResolutionRate())
                .accountabilityScore(accountabilityScore)
                .userRating(userRating)
                .hasUserRated(hasUserRated)
                .build();
    }

    /**
     * Get issue stats for a representative
     */
    public IssueStats getIssueStats(RepresentativeType type, Long representativeId, String representativeName) {
        Long totalIssues = 0L;
        Long resolvedIssues = 0L;
        Long pendingIssues = 0L;
        Long inProgressIssues = 0L;

        // Query based on representative type
        switch (type) {
            case WARD_COUNCILLOR:
                totalIssues = issueRepository.countByCouncillorId(representativeId);
                resolvedIssues = issueRepository.countByCouncillorIdResolved(representativeId);
                pendingIssues = issueRepository.countByCouncillorIdPending(representativeId);
                inProgressIssues = issueRepository.countByCouncillorIdInProgress(representativeId);
                break;
            case MLA:
                totalIssues = issueRepository.countByMlaId(representativeId);
                resolvedIssues = issueRepository.countByMlaIdResolved(representativeId);
                pendingIssues = issueRepository.countByMlaIdPending(representativeId);
                inProgressIssues = issueRepository.countByMlaIdInProgress(representativeId);
                break;
            case MP:
                totalIssues = issueRepository.countByMpId(representativeId);
                resolvedIssues = issueRepository.countByMpIdResolved(representativeId);
                pendingIssues = issueRepository.countByMpIdPending(representativeId);
                inProgressIssues = issueRepository.countByMpIdInProgress(representativeId);
                break;
            default:
                // For Mayor, CM, PM - aggregate by constituency/state
                break;
        }

        double resolutionRate = totalIssues > 0 ? (resolvedIssues * 100.0 / totalIssues) : 0.0;

        return IssueStats.builder()
                .representativeType(type)
                .representativeId(representativeId)
                .representativeName(representativeName)
                .totalIssues(totalIssues)
                .resolvedIssues(resolvedIssues)
                .pendingIssues(pendingIssues)
                .inProgressIssues(inProgressIssues)
                .resolutionRate(Math.round(resolutionRate * 10.0) / 10.0)
                .build();
    }

    /**
     * Calculate accountability score (0-100) based on ratings and issue resolution
     */
    private int calculateAccountabilityScore(RatingStats ratingStats, IssueStats issueStats) {
        // Weight: 60% approval rating, 40% issue resolution
        double ratingScore = 0;
        if (ratingStats.getAverageRating() != null && ratingStats.getTotalRatings() > 0) {
            ratingScore = (ratingStats.getAverageRating() / 5.0) * 60;
        }

        double resolutionScore = 0;
        if (issueStats.getResolutionRate() != null) {
            resolutionScore = (issueStats.getResolutionRate() / 100.0) * 40;
        }

        return (int) Math.round(ratingScore + resolutionScore);
    }

    /**
     * Get recent reviews for a representative
     */
    public List<RatingResponse> getRecentReviews(RepresentativeType type, Long representativeId, int limit) {
        return ratingRepository.getRecentReviews(type, representativeId)
                .stream()
                .limit(limit)
                .map(RatingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get top rated representatives by type
     */
    public List<RepresentativeStats> getTopRated(RepresentativeType type, int limit, Long minRatings) {
        return ratingRepository.getTopRatedByType(type, minRatings)
                .stream()
                .limit(limit)
                .map(arr -> RepresentativeStats.builder()
                        .representativeId(((Number) arr[0]).longValue())
                        .name((String) arr[1])
                        .party((String) arr[2])
                        .constituency((String) arr[3])
                        .approvalRating(((Number) arr[4]).doubleValue())
                        .totalRatings(((Number) arr[5]).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Delete user's rating
     */
    @Transactional
    public void deleteRating(Long userId, RepresentativeType type, Long representativeId) {
        ratingRepository.deleteByUserIdAndRepresentativeTypeAndRepresentativeId(userId, type, representativeId);
        log.info("Deleted rating for user {} on {} {}", userId, type, representativeId);
    }

    /**
     * Convert aspect ratings map to string for storage
     */
    private String convertAspectRatingsToString(java.util.Map<String, Integer> aspectRatings) {
        if (aspectRatings == null || aspectRatings.isEmpty()) return null;
        return aspectRatings.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(","));
    }
}
