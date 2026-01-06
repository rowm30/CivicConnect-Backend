package com.civicconnect.api.entity;

import com.civicconnect.api.entity.analytics.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Issue entity for civic issues reported by users
 * Supports heat score calculation for Issue Pulse feature
 */
@Entity
@Table(name = "issues", indexes = {
    @Index(name = "idx_issue_status", columnList = "status"),
    @Index(name = "idx_issue_category", columnList = "category"),
    @Index(name = "idx_issue_reporter", columnList = "reporter_id"),
    @Index(name = "idx_issue_location", columnList = "latitude, longitude"),
    @Index(name = "idx_issue_created", columnList = "created_at DESC"),
    @Index(name = "idx_issue_pc", columnList = "parliamentary_constituency"),
    @Index(name = "idx_issue_ac", columnList = "assembly_constituency")
})
@Getter
@Setter
@NoArgsConstructor
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "audio_url")
    private String audioUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueStatus status = IssueStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private IssuePriority priority = IssuePriority.MEDIUM;

    // Location
    private Double latitude;
    private Double longitude;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "district_name")
    private String districtName;

    @Column(name = "state_name")
    private String stateName;

    // Constituency information (for location-based filtering)
    @Column(name = "parliamentary_constituency")
    private String parliamentaryConstituency;

    @Column(name = "assembly_constituency")
    private String assemblyConstituency;

    // MLA Information (auto-populated from location)
    @Column(name = "mla_id")
    private Long mlaId;

    @Column(name = "mla_name")
    private String mlaName;

    @Column(name = "mla_party")
    private String mlaParty;

    // MP Information (auto-populated from location)
    @Column(name = "mp_id")
    private Long mpId;

    @Column(name = "mp_name")
    private String mpName;

    @Column(name = "mp_party")
    private String mpParty;

    // Ward Councillor Information (auto-populated from location for municipal areas)
    @Column(name = "councillor_id")
    private Long councillorId;

    @Column(name = "councillor_name")
    private String councillorName;

    @Column(name = "councillor_party")
    private String councillorParty;

    @Column(name = "ward_no")
    private Integer wardNo;

    @Column(name = "ward_name")
    private String wardName;

    // Department/Official assignment
    @Column(name = "department_name")
    private String departmentName;

    @Column(name = "assigned_official_name")
    private String assignedOfficialName;

    @Column(name = "assigned_official_id")
    private Long assignedOfficialId;

    // Voting stats (denormalized for performance)
    @Column(name = "upvote_count")
    private Integer upvoteCount = 0;

    @Column(name = "downvote_count")
    private Integer downvoteCount = 0;

    // Reporter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private AppUser reporter;

    // Votes
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IssueVote> votes = new ArrayList<>();

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Tracking ID for users to track their issues
    @Column(name = "tracking_id", unique = true)
    private String trackingId;

    /**
     * Calculate heat score as percentage (0-100)
     * Higher upvote ratio = hotter issue
     */
    public float getHeatScore() {
        int totalVotes = upvoteCount + downvoteCount + 1; // +1 to avoid division by zero
        return (upvoteCount * 100.0f) / totalVotes;
    }

    /**
     * Get heat level for UI display
     */
    public HeatLevel getHeatLevel() {
        float score = getHeatScore();
        if (score > 70) return HeatLevel.HOT;
        if (score > 40) return HeatLevel.WARM;
        return HeatLevel.COLD;
    }

    public enum IssueCategory {
        ROADS,
        WATER,
        ELECTRICITY,
        WASTE,
        SAFETY,
        PARKS,
        BUILDING,
        TRAFFIC,
        NOISE,
        OTHER
    }

    public enum IssueStatus {
        PENDING,
        ACKNOWLEDGED,
        IN_PROGRESS,
        RESOLVED,
        CLOSED
    }

    public enum IssuePriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }

    public enum HeatLevel {
        HOT,    // > 70%
        WARM,   // 40-70%
        COLD    // < 40%
    }
}
