package com.civicconnect.api.entity;

import com.civicconnect.api.entity.analytics.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks user applications for benefits
 */
@Entity
@Table(name = "user_benefit_applications",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_benefit",
        columnNames = {"user_id", "benefit_id"}
    ),
    indexes = {
        @Index(name = "idx_uba_user", columnList = "user_id"),
        @Index(name = "idx_uba_benefit", columnList = "benefit_id"),
        @Index(name = "idx_uba_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class UserBenefitApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benefit_id", nullable = false)
    private Benefit benefit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.SAVED;

    @Column(name = "application_reference")
    private String applicationReference;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    public enum ApplicationStatus {
        SAVED,          // User saved benefit for later
        APPLIED,        // User applied
        UNDER_REVIEW,   // Application under review
        APPROVED,       // Application approved
        REJECTED,       // Application rejected
        DISBURSED       // Benefit disbursed
    }
}
