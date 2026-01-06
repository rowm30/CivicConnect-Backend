package com.civicconnect.api.dto;

import com.civicconnect.api.entity.RepresentativeRating;
import com.civicconnect.api.entity.RepresentativeRating.RepresentativeType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTOs for representative rating API
 */
public class RepresentativeRatingDTO {

    /**
     * Request to submit a rating
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingRequest {
        private RepresentativeType representativeType;
        private Long representativeId;
        private String representativeName;
        private String partyName;
        private String constituencyName;
        private Integer rating; // 1-5
        private String comment;
        private Map<String, Integer> aspectRatings; // e.g., {"accessibility": 4, "responsiveness": 3}
        private Boolean isAnonymous;
    }

    /**
     * Response for a single rating
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingResponse {
        private Long id;
        private Long userId;
        private String userName;
        private RepresentativeType representativeType;
        private Long representativeId;
        private String representativeName;
        private String partyName;
        private String constituencyName;
        private Integer rating;
        private String comment;
        private Map<String, Integer> aspectRatings;
        private Boolean isAnonymous;
        private Boolean isVerified;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static RatingResponse fromEntity(RepresentativeRating entity) {
            return RatingResponse.builder()
                    .id(entity.getId())
                    .userId(entity.getIsAnonymous() ? null : entity.getUser().getId())
                    .userName(entity.getIsAnonymous() ? "Anonymous" : entity.getUser().getName())
                    .representativeType(entity.getRepresentativeType())
                    .representativeId(entity.getRepresentativeId())
                    .representativeName(entity.getRepresentativeName())
                    .partyName(entity.getPartyName())
                    .constituencyName(entity.getConstituencyName())
                    .rating(entity.getRating())
                    .comment(entity.getComment())
                    .isAnonymous(entity.getIsAnonymous())
                    .isVerified(entity.getIsVerified())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }

    /**
     * Aggregate rating stats for a representative
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingStats {
        private RepresentativeType representativeType;
        private Long representativeId;
        private String representativeName;
        private String partyName;
        private String constituencyName;
        private Double averageRating;
        private Long totalRatings;
        private Long verifiedRatings;
        private Long fiveStarCount;
        private Long fourStarCount;
        private Long threeStarCount;
        private Long twoStarCount;
        private Long oneStarCount;
        private String approvalLevel; // "Excellent", "Good", "Average", "Poor", "Very Poor"

        public static String calculateApprovalLevel(Double avgRating) {
            if (avgRating == null || avgRating == 0) return "Not Rated";
            if (avgRating >= 4.5) return "Excellent";
            if (avgRating >= 3.5) return "Good";
            if (avgRating >= 2.5) return "Average";
            if (avgRating >= 1.5) return "Poor";
            return "Very Poor";
        }
    }

    /**
     * Issue stats for a representative
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueStats {
        private RepresentativeType representativeType;
        private Long representativeId;
        private String representativeName;
        private Long totalIssues;
        private Long resolvedIssues;
        private Long pendingIssues;
        private Long inProgressIssues;
        private Double resolutionRate; // percentage
        private Double avgResolutionDays; // average days to resolve
    }

    /**
     * Combined stats for Gov Map display
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepresentativeStats {
        private RepresentativeType representativeType;
        private Long representativeId;
        private String name;
        private String party;
        private String constituency;
        private String photoUrl;
        private String designation; // "Ward Councillor", "MLA", "MP", etc.

        // Rating stats
        private Double approvalRating;
        private Long totalRatings;
        private String approvalLevel;

        // Issue stats
        private Long issuesRegistered;
        private Long issuesResolved;
        private Double resolutionRate;

        // Accountability score (0-100)
        private Integer accountabilityScore;

        // User's own rating (if exists)
        private Integer userRating;
        private Boolean hasUserRated;
    }

    /**
     * Gov Map hierarchy response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GovMapHierarchy {
        private String mode; // "LOCAL", "STATE", "NATIONAL"
        private String stateName;
        private String constituencyName;

        // Citizen info (the user)
        private CitizenNode citizen;

        // Representatives in hierarchy order (bottom to top)
        private RepresentativeStats councillor; // Local mode
        private RepresentativeStats mayor;      // Local mode
        private RepresentativeStats mla;        // State mode
        private RepresentativeStats cm;         // State mode
        private RepresentativeStats mp;         // National mode
        private RepresentativeStats pm;         // National mode

        // Summary stats
        private Long totalIssuesInConstituency;
        private Long resolvedIssuesInConstituency;
        private Long activeVotersInConstituency;
    }

    /**
     * Citizen node in the hierarchy
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CitizenNode {
        private Long userId;
        private String name;
        private String constituency;
        private Long issuesReported;
        private Long issuesResolved;
        private Long ratingsGiven;
        private String voterPowerMessage; // "Your vote empowers..."
    }
}
