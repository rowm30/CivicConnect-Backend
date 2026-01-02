package com.civicconnect.api.dto;

import com.civicconnect.api.entity.Issue;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Issue DTO for API responses
 * Includes heat score and level for Issue Pulse feature
 */
@Data
@Builder
public class IssueDTO {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String category;
    private String status;
    private String priority;

    // Location
    private Double latitude;
    private Double longitude;
    private String locationName;
    private String districtName;
    private String stateName;

    // Constituency information
    private String parliamentaryConstituency;
    private String assemblyConstituency;

    // MLA Information
    private Long mlaId;
    private String mlaName;
    private String mlaParty;

    // MP Information
    private Long mpId;
    private String mpName;
    private String mpParty;

    // Assignment
    private String departmentName;
    private String assignedOfficialName;
    private Long assignedOfficialId;

    // Voting
    private Integer upvoteCount;
    private Integer downvoteCount;
    private Float heatScore;
    private String heatLevel;

    // User's vote on this issue (if authenticated)
    private String userVote; // "UPVOTE", "DOWNVOTE", or null

    // Reporter info
    private Long reporterId;
    private String reporterName;

    // Tracking
    private String trackingId;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    /**
     * Convert Issue entity to DTO
     */
    public static IssueDTO fromEntity(Issue issue) {
        return fromEntity(issue, null);
    }

    /**
     * Convert Issue entity to DTO with user's vote status
     */
    public static IssueDTO fromEntity(Issue issue, String userVote) {
        return IssueDTO.builder()
                .id(issue.getId())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .imageUrl(issue.getImageUrl())
                .category(issue.getCategory().name())
                .status(issue.getStatus().name())
                .priority(issue.getPriority() != null ? issue.getPriority().name() : null)
                .latitude(issue.getLatitude())
                .longitude(issue.getLongitude())
                .locationName(issue.getLocationName())
                .districtName(issue.getDistrictName())
                .stateName(issue.getStateName())
                .parliamentaryConstituency(issue.getParliamentaryConstituency())
                .assemblyConstituency(issue.getAssemblyConstituency())
                .mlaId(issue.getMlaId())
                .mlaName(issue.getMlaName())
                .mlaParty(issue.getMlaParty())
                .mpId(issue.getMpId())
                .mpName(issue.getMpName())
                .mpParty(issue.getMpParty())
                .departmentName(issue.getDepartmentName())
                .assignedOfficialName(issue.getAssignedOfficialName())
                .assignedOfficialId(issue.getAssignedOfficialId())
                .upvoteCount(issue.getUpvoteCount())
                .downvoteCount(issue.getDownvoteCount())
                .heatScore(issue.getHeatScore())
                .heatLevel(issue.getHeatLevel().name())
                .userVote(userVote)
                .reporterId(issue.getReporter() != null ? issue.getReporter().getId() : null)
                .reporterName(issue.getReporter() != null ? issue.getReporter().getName() : null)
                .trackingId(issue.getTrackingId())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .resolvedAt(issue.getResolvedAt())
                .build();
    }
}
