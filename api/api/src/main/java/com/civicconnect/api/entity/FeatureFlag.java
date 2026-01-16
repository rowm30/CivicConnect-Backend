package com.civicconnect.api.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing a feature flag that can be enabled/disabled globally.
 * Feature flags control access to specific features in the application.
 */
@Entity
@Table(name = "feature_flags")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlag extends BaseEntity {

    /**
     * Unique key identifier for the feature (e.g., "code_extractor", "gov_map", "benefits")
     */
    @Column(name = "feature_key", nullable = false, unique = true, length = 100)
    private String featureKey;

    /**
     * Human-readable name for the feature
     */
    @Column(name = "feature_name", nullable = false, length = 200)
    private String featureName;

    /**
     * Description of what this feature does
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Whether this feature is enabled globally for all users
     */
    @Column(name = "enabled_globally", nullable = false)
    @Builder.Default
    private Boolean enabledGlobally = true;

    /**
     * Category of the feature for grouping in admin panel
     */
    @Column(name = "category", length = 100)
    private String category;

    /**
     * Display order for sorting in admin panel
     */
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * Icon name for admin panel display (Ant Design icon name)
     */
    @Column(name = "icon", length = 50)
    private String icon;

    /**
     * Minimum app version required for this feature (e.g., "1.2.0")
     */
    @Column(name = "min_app_version", length = 20)
    private String minAppVersion;

    /**
     * Platform-specific flag: ANDROID, IOS, WEB, ALL
     */
    @Column(name = "platform", length = 20)
    @Builder.Default
    private String platform = "ALL";
}
