package com.civicconnect.api.service;

import com.civicconnect.api.dto.FeatureFlagDTO;
import com.civicconnect.api.entity.FeatureFlag;
import com.civicconnect.api.entity.UserFeatureOverride;
import com.civicconnect.api.repository.FeatureFlagRepository;
import com.civicconnect.api.repository.UserFeatureOverrideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;
    private final UserFeatureOverrideRepository userFeatureOverrideRepository;

    // ==================== Feature Flag CRUD ====================

    /**
     * Get all feature flags with statistics
     */
    public List<FeatureFlagDTO.FeatureFlagResponse> getAllFeatureFlags() {
        return featureFlagRepository.findAllByOrderByCategoryAscDisplayOrderAsc()
                .stream()
                .map(this::toFeatureFlagResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get feature flag by ID
     */
    public FeatureFlagDTO.FeatureFlagResponse getFeatureFlagById(Long id) {
        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feature flag not found: " + id));
        return toFeatureFlagResponse(flag);
    }

    /**
     * Get feature flag by key
     */
    public FeatureFlagDTO.FeatureFlagResponse getFeatureFlagByKey(String key) {
        FeatureFlag flag = featureFlagRepository.findByFeatureKey(key)
                .orElseThrow(() -> new RuntimeException("Feature flag not found: " + key));
        return toFeatureFlagResponse(flag);
    }

    /**
     * Create a new feature flag
     */
    @Transactional
    public FeatureFlagDTO.FeatureFlagResponse createFeatureFlag(FeatureFlagDTO.CreateFeatureFlagRequest request) {
        if (featureFlagRepository.existsByFeatureKey(request.getFeatureKey())) {
            throw new RuntimeException("Feature key already exists: " + request.getFeatureKey());
        }

        FeatureFlag flag = FeatureFlag.builder()
                .featureKey(request.getFeatureKey())
                .featureName(request.getFeatureName())
                .description(request.getDescription())
                .enabledGlobally(request.getEnabledGlobally() != null ? request.getEnabledGlobally() : true)
                .category(request.getCategory())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .icon(request.getIcon())
                .minAppVersion(request.getMinAppVersion())
                .platform(request.getPlatform() != null ? request.getPlatform() : "ALL")
                .build();

        flag = featureFlagRepository.save(flag);
        log.info("Created feature flag: {}", flag.getFeatureKey());
        return toFeatureFlagResponse(flag);
    }

    /**
     * Update a feature flag
     */
    @Transactional
    public FeatureFlagDTO.FeatureFlagResponse updateFeatureFlag(Long id, FeatureFlagDTO.UpdateFeatureFlagRequest request) {
        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feature flag not found: " + id));

        if (request.getFeatureName() != null) flag.setFeatureName(request.getFeatureName());
        if (request.getDescription() != null) flag.setDescription(request.getDescription());
        if (request.getEnabledGlobally() != null) flag.setEnabledGlobally(request.getEnabledGlobally());
        if (request.getCategory() != null) flag.setCategory(request.getCategory());
        if (request.getDisplayOrder() != null) flag.setDisplayOrder(request.getDisplayOrder());
        if (request.getIcon() != null) flag.setIcon(request.getIcon());
        if (request.getMinAppVersion() != null) flag.setMinAppVersion(request.getMinAppVersion());
        if (request.getPlatform() != null) flag.setPlatform(request.getPlatform());
        if (request.getIsActive() != null) flag.setIsActive(request.getIsActive());

        flag = featureFlagRepository.save(flag);
        log.info("Updated feature flag: {}", flag.getFeatureKey());
        return toFeatureFlagResponse(flag);
    }

    /**
     * Toggle global status of a feature flag
     */
    @Transactional
    public FeatureFlagDTO.FeatureFlagResponse toggleGlobalStatus(Long id, Boolean enabled) {
        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feature flag not found: " + id));

        flag.setEnabledGlobally(enabled);
        flag = featureFlagRepository.save(flag);
        log.info("Toggled feature flag {} to: {}", flag.getFeatureKey(), enabled);
        return toFeatureFlagResponse(flag);
    }

    /**
     * Delete a feature flag (soft delete)
     */
    @Transactional
    public void deleteFeatureFlag(Long id) {
        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feature flag not found: " + id));

        flag.setIsActive(false);
        featureFlagRepository.save(flag);
        log.info("Soft deleted feature flag: {}", flag.getFeatureKey());
    }

    // ==================== User Overrides ====================

    /**
     * Get all overrides for a feature flag
     */
    public List<FeatureFlagDTO.UserOverrideResponse> getOverridesByFeatureFlag(Long featureFlagId) {
        return userFeatureOverrideRepository.findByFeatureFlagId(featureFlagId)
                .stream()
                .map(this::toUserOverrideResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all overrides for a user
     */
    public List<FeatureFlagDTO.UserOverrideResponse> getOverridesByUser(Long userId) {
        return userFeatureOverrideRepository.findByUserId(userId)
                .stream()
                .map(this::toUserOverrideResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create or update a user override
     */
    @Transactional
    public FeatureFlagDTO.UserOverrideResponse setUserOverride(FeatureFlagDTO.UserOverrideRequest request) {
        FeatureFlag flag = featureFlagRepository.findById(request.getFeatureFlagId())
                .orElseThrow(() -> new RuntimeException("Feature flag not found: " + request.getFeatureFlagId()));

        Optional<UserFeatureOverride> existingOverride = userFeatureOverrideRepository
                .findByUserIdAndFeatureFlagId(request.getUserId(), request.getFeatureFlagId());

        UserFeatureOverride override;
        if (existingOverride.isPresent()) {
            override = existingOverride.get();
            override.setEnabled(request.getEnabled());
            override.setReason(request.getReason());
        } else {
            override = UserFeatureOverride.builder()
                    .userId(request.getUserId())
                    .featureFlag(flag)
                    .enabled(request.getEnabled())
                    .reason(request.getReason())
                    .createdBy(request.getCreatedBy())
                    .build();
        }

        override = userFeatureOverrideRepository.save(override);
        log.info("Set user override for user {} on feature {}: {}",
                request.getUserId(), flag.getFeatureKey(), request.getEnabled());
        return toUserOverrideResponse(override);
    }

    /**
     * Bulk create user overrides
     */
    @Transactional
    public List<FeatureFlagDTO.UserOverrideResponse> setBulkUserOverrides(FeatureFlagDTO.BulkUserOverrideRequest request) {
        FeatureFlag flag = featureFlagRepository.findById(request.getFeatureFlagId())
                .orElseThrow(() -> new RuntimeException("Feature flag not found: " + request.getFeatureFlagId()));

        List<FeatureFlagDTO.UserOverrideResponse> results = new ArrayList<>();

        for (Long userId : request.getUserIds()) {
            Optional<UserFeatureOverride> existingOverride = userFeatureOverrideRepository
                    .findByUserIdAndFeatureFlagId(userId, request.getFeatureFlagId());

            UserFeatureOverride override;
            if (existingOverride.isPresent()) {
                override = existingOverride.get();
                override.setEnabled(request.getEnabled());
                override.setReason(request.getReason());
            } else {
                override = UserFeatureOverride.builder()
                        .userId(userId)
                        .featureFlag(flag)
                        .enabled(request.getEnabled())
                        .reason(request.getReason())
                        .createdBy(request.getCreatedBy())
                        .build();
            }

            override = userFeatureOverrideRepository.save(override);
            results.add(toUserOverrideResponse(override));
        }

        log.info("Created {} bulk user overrides for feature {}", results.size(), flag.getFeatureKey());
        return results;
    }

    /**
     * Remove a user override
     */
    @Transactional
    public void removeUserOverride(Long overrideId) {
        UserFeatureOverride override = userFeatureOverrideRepository.findById(overrideId)
                .orElseThrow(() -> new RuntimeException("User override not found: " + overrideId));

        userFeatureOverrideRepository.delete(override);
        log.info("Removed user override {} for user {} on feature {}",
                overrideId, override.getUserId(), override.getFeatureFlag().getFeatureKey());
    }

    /**
     * Remove user override by user ID and feature flag ID
     */
    @Transactional
    public void removeUserOverrideByUserAndFeature(Long userId, Long featureFlagId) {
        userFeatureOverrideRepository.findByUserIdAndFeatureFlagId(userId, featureFlagId)
                .ifPresent(override -> {
                    userFeatureOverrideRepository.delete(override);
                    log.info("Removed user override for user {} on feature {}",
                            userId, override.getFeatureFlag().getFeatureKey());
                });
    }

    // ==================== User Feature Status (for mobile app) ====================

    /**
     * Check if a specific feature is enabled for a user
     */
    public boolean isFeatureEnabledForUser(Long userId, String featureKey) {
        Optional<FeatureFlag> flagOpt = featureFlagRepository.findByFeatureKey(featureKey);
        if (flagOpt.isEmpty() || !flagOpt.get().getIsActive()) {
            return false;
        }

        FeatureFlag flag = flagOpt.get();

        // Check for user-specific override first
        Optional<UserFeatureOverride> override = userFeatureOverrideRepository
                .findByUserIdAndFeatureKey(userId, featureKey);

        if (override.isPresent()) {
            return override.get().getEnabled();
        }

        // Fall back to global setting
        return flag.getEnabledGlobally();
    }

    /**
     * Get all features with their status for a specific user
     */
    public FeatureFlagDTO.UserFeaturesResponse getUserFeatures(Long userId, String platform) {
        List<FeatureFlag> flags;
        if (platform != null && !platform.isEmpty()) {
            flags = featureFlagRepository.findByPlatform(platform);
        } else {
            flags = featureFlagRepository.findByIsActiveTrueOrderByCategoryAscDisplayOrderAsc();
        }

        List<FeatureFlagDTO.UserFeatureStatusResponse> features = flags.stream()
                .map(flag -> {
                    // Check for user-specific override
                    Optional<UserFeatureOverride> override = userFeatureOverrideRepository
                            .findByUserIdAndFeatureFlagId(userId, flag.getId());

                    boolean enabled = override.isPresent()
                            ? override.get().getEnabled()
                            : flag.getEnabledGlobally();

                    return FeatureFlagDTO.UserFeatureStatusResponse.builder()
                            .featureKey(flag.getFeatureKey())
                            .featureName(flag.getFeatureName())
                            .enabled(enabled)
                            .icon(flag.getIcon())
                            .minAppVersion(flag.getMinAppVersion())
                            .build();
                })
                .collect(Collectors.toList());

        return FeatureFlagDTO.UserFeaturesResponse.builder()
                .userId(userId)
                .features(features)
                .build();
    }

    // ==================== Statistics ====================

    /**
     * Get feature flag statistics for admin dashboard
     */
    public FeatureFlagDTO.FeatureFlagStats getStats() {
        List<FeatureFlag> allFlags = featureFlagRepository.findAll();

        long total = allFlags.size();
        long enabled = allFlags.stream().filter(f -> Boolean.TRUE.equals(f.getEnabledGlobally())).count();
        long disabled = total - enabled;
        long totalOverrides = userFeatureOverrideRepository.count();
        List<String> categories = featureFlagRepository.findAllCategories();

        return FeatureFlagDTO.FeatureFlagStats.builder()
                .totalFeatures(total)
                .enabledFeatures(enabled)
                .disabledFeatures(disabled)
                .totalUserOverrides(totalOverrides)
                .categories(categories)
                .build();
    }

    /**
     * Get all distinct categories
     */
    public List<String> getAllCategories() {
        return featureFlagRepository.findAllCategories();
    }

    // ==================== Converters ====================

    private FeatureFlagDTO.FeatureFlagResponse toFeatureFlagResponse(FeatureFlag flag) {
        return FeatureFlagDTO.FeatureFlagResponse.builder()
                .id(flag.getId())
                .featureKey(flag.getFeatureKey())
                .featureName(flag.getFeatureName())
                .description(flag.getDescription())
                .enabledGlobally(flag.getEnabledGlobally())
                .category(flag.getCategory())
                .displayOrder(flag.getDisplayOrder())
                .icon(flag.getIcon())
                .minAppVersion(flag.getMinAppVersion())
                .platform(flag.getPlatform())
                .isActive(flag.getIsActive())
                .createdAt(flag.getCreatedAt())
                .updatedAt(flag.getUpdatedAt())
                .totalUserOverrides(userFeatureOverrideRepository.countByFeatureFlagId(flag.getId()))
                .enabledOverrides(userFeatureOverrideRepository.countEnabledByFeatureFlagId(flag.getId()))
                .disabledOverrides(userFeatureOverrideRepository.countDisabledByFeatureFlagId(flag.getId()))
                .build();
    }

    private FeatureFlagDTO.UserOverrideResponse toUserOverrideResponse(UserFeatureOverride override) {
        return FeatureFlagDTO.UserOverrideResponse.builder()
                .id(override.getId())
                .userId(override.getUserId())
                .featureFlagId(override.getFeatureFlag().getId())
                .featureKey(override.getFeatureFlag().getFeatureKey())
                .featureName(override.getFeatureFlag().getFeatureName())
                .enabled(override.getEnabled())
                .reason(override.getReason())
                .createdBy(override.getCreatedBy())
                .createdAt(override.getCreatedAt())
                .updatedAt(override.getUpdatedAt())
                .build();
    }
}
