package com.civicconnect.api.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity for user-specific feature flag overrides.
 * Allows enabling/disabling features for individual users regardless of global setting.
 */
@Entity
@Table(name = "user_feature_overrides",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "feature_flag_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFeatureOverride extends BaseEntity {

    /**
     * The user ID this override applies to
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * The feature flag being overridden
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_flag_id", nullable = false)
    private FeatureFlag featureFlag;

    /**
     * Whether this feature is enabled for this specific user
     * This takes precedence over the global setting
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    /**
     * Reason for this override (for admin audit purposes)
     */
    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * Admin user who created this override
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;
}
