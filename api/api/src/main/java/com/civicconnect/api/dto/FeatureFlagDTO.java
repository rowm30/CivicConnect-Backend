package com.civicconnect.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class FeatureFlagDTO {

    /**
     * Response DTO for feature flag details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureFlagResponse {
        private Long id;
        private String featureKey;
        private String featureName;
        private String description;
        private Boolean enabledGlobally;
        private String category;
        private Integer displayOrder;
        private String icon;
        private String minAppVersion;
        private String platform;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        // Stats for admin panel
        private Long totalUserOverrides;
        private Long enabledOverrides;
        private Long disabledOverrides;
    }

    /**
     * Request DTO for creating a new feature flag
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateFeatureFlagRequest {
        private String featureKey;
        private String featureName;
        private String description;
        private Boolean enabledGlobally;
        private String category;
        private Integer displayOrder;
        private String icon;
        private String minAppVersion;
        private String platform;
    }

    /**
     * Request DTO for updating a feature flag
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateFeatureFlagRequest {
        private String featureName;
        private String description;
        private Boolean enabledGlobally;
        private String category;
        private Integer displayOrder;
        private String icon;
        private String minAppVersion;
        private String platform;
        private Boolean isActive;
    }

    /**
     * Request DTO for toggling global status
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToggleGlobalRequest {
        private Boolean enabledGlobally;
    }

    /**
     * Response DTO for user feature override
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserOverrideResponse {
        private Long id;
        private Long userId;
        private Long featureFlagId;
        private String featureKey;
        private String featureName;
        private Boolean enabled;
        private String reason;
        private String createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * Request DTO for creating/updating user override
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserOverrideRequest {
        private Long userId;
        private Long featureFlagId;
        private Boolean enabled;
        private String reason;
        private String createdBy;
    }

    /**
     * Bulk user override request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkUserOverrideRequest {
        private List<Long> userIds;
        private Long featureFlagId;
        private Boolean enabled;
        private String reason;
        private String createdBy;
    }

    /**
     * Response for checking user's feature status
     * Used by mobile app to determine which features to show
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserFeatureStatusResponse {
        private String featureKey;
        private String featureName;
        private Boolean enabled;
        private String icon;
        private String minAppVersion;
    }

    /**
     * Response containing all features for a user
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserFeaturesResponse {
        private Long userId;
        private List<UserFeatureStatusResponse> features;
    }

    /**
     * Summary statistics for admin dashboard
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureFlagStats {
        private Long totalFeatures;
        private Long enabledFeatures;
        private Long disabledFeatures;
        private Long totalUserOverrides;
        private List<String> categories;
    }
}
