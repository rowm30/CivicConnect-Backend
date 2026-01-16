package com.civicconnect.api.entity;

import com.civicconnect.api.entity.analytics.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity to store user-submitted information about government nodes
 * Users can contribute info about officials where data is not available
 */
@Entity
@Table(name = "gov_node_info_submissions", indexes = {
    @Index(name = "idx_submission_node_type", columnList = "nodeType"),
    @Index(name = "idx_submission_status", columnList = "status"),
    @Index(name = "idx_submission_user", columnList = "submitted_by_id"),
    @Index(name = "idx_submission_state", columnList = "stateName"),
    @Index(name = "idx_submission_created", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovNodeInfoSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type of government node: WARD_COUNCILLOR, MLA, MP, MAYOR, CHIEF_MINISTER, PRIME_MINISTER
     */
    @Column(nullable = false)
    private String nodeType;

    /**
     * Hierarchy mode: LOCAL, STATE, NATIONAL
     */
    @Column(nullable = false)
    private String hierarchyMode;

    // Location context
    private String stateName;
    private String stateCode;
    private String cityName;
    private String districtName;
    private String wardName;
    private Integer wardNumber;
    private String assemblyConstituency;
    private String parliamentaryConstituency;

    // Submitted official information
    @Column(nullable = false)
    private String officialName;

    private String party;
    private String partyAbbreviation;
    private String designation;
    private String photoUrl;

    // Contact information
    private String email;
    private String phone;
    private String address;
    private String twitterHandle;

    // Additional info
    @Column(columnDefinition = "TEXT")
    private String additionalInfo;

    // Source/verification info
    private String sourceUrl;
    private String sourceDescription;

    // Submission status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.PENDING;

    // Admin review
    private String reviewNotes;
    private Long reviewedById;
    private LocalDateTime reviewedAt;

    // Submitter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id", nullable = false)
    private AppUser submittedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum SubmissionStatus {
        PENDING,      // Awaiting review
        APPROVED,     // Verified and added to database
        REJECTED,     // Invalid/incorrect information
        DUPLICATE     // Already exists in database
    }
}
