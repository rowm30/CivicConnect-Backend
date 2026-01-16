package com.civicconnect.api.controller;

import com.civicconnect.api.dto.FeatureFlagDTO;
import com.civicconnect.api.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for feature flag management.
 * Provides endpoints for admin panel and mobile app feature checks.
 */
@RestController
@RequestMapping("/api/feature-flags")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    // ==================== Admin Endpoints ====================

    /**
     * Get all feature flags (for admin panel)
     */
    @GetMapping
    public ResponseEntity<List<FeatureFlagDTO.FeatureFlagResponse>> getAllFeatureFlags() {
        return ResponseEntity.ok(featureFlagService.getAllFeatureFlags());
    }

    /**
     * Get feature flag by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<FeatureFlagDTO.FeatureFlagResponse> getFeatureFlagById(@PathVariable Long id) {
        return ResponseEntity.ok(featureFlagService.getFeatureFlagById(id));
    }

    /**
     * Get feature flag by key
     */
    @GetMapping("/key/{key}")
    public ResponseEntity<FeatureFlagDTO.FeatureFlagResponse> getFeatureFlagByKey(@PathVariable String key) {
        return ResponseEntity.ok(featureFlagService.getFeatureFlagByKey(key));
    }

    /**
     * Create a new feature flag
     */
    @PostMapping
    public ResponseEntity<FeatureFlagDTO.FeatureFlagResponse> createFeatureFlag(
            @RequestBody FeatureFlagDTO.CreateFeatureFlagRequest request) {
        return ResponseEntity.ok(featureFlagService.createFeatureFlag(request));
    }

    /**
     * Update a feature flag
     */
    @PutMapping("/{id}")
    public ResponseEntity<FeatureFlagDTO.FeatureFlagResponse> updateFeatureFlag(
            @PathVariable Long id,
            @RequestBody FeatureFlagDTO.UpdateFeatureFlagRequest request) {
        return ResponseEntity.ok(featureFlagService.updateFeatureFlag(id, request));
    }

    /**
     * Toggle global status of a feature flag
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<FeatureFlagDTO.FeatureFlagResponse> toggleGlobalStatus(
            @PathVariable Long id,
            @RequestBody FeatureFlagDTO.ToggleGlobalRequest request) {
        return ResponseEntity.ok(featureFlagService.toggleGlobalStatus(id, request.getEnabledGlobally()));
    }

    /**
     * Delete a feature flag (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteFeatureFlag(@PathVariable Long id) {
        featureFlagService.deleteFeatureFlag(id);
        return ResponseEntity.ok(Map.of("message", "Feature flag deleted successfully"));
    }

    /**
     * Get feature flag statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<FeatureFlagDTO.FeatureFlagStats> getStats() {
        return ResponseEntity.ok(featureFlagService.getStats());
    }

    /**
     * Get all categories
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(featureFlagService.getAllCategories());
    }

    // ==================== User Override Endpoints ====================

    /**
     * Get all overrides for a feature flag
     */
    @GetMapping("/{id}/overrides")
    public ResponseEntity<List<FeatureFlagDTO.UserOverrideResponse>> getOverridesByFeatureFlag(@PathVariable Long id) {
        return ResponseEntity.ok(featureFlagService.getOverridesByFeatureFlag(id));
    }

    /**
     * Get all overrides for a user
     */
    @GetMapping("/user/{userId}/overrides")
    public ResponseEntity<List<FeatureFlagDTO.UserOverrideResponse>> getOverridesByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(featureFlagService.getOverridesByUser(userId));
    }

    /**
     * Create or update a user override
     */
    @PostMapping("/overrides")
    public ResponseEntity<FeatureFlagDTO.UserOverrideResponse> setUserOverride(
            @RequestBody FeatureFlagDTO.UserOverrideRequest request) {
        return ResponseEntity.ok(featureFlagService.setUserOverride(request));
    }

    /**
     * Bulk create user overrides
     */
    @PostMapping("/overrides/bulk")
    public ResponseEntity<List<FeatureFlagDTO.UserOverrideResponse>> setBulkUserOverrides(
            @RequestBody FeatureFlagDTO.BulkUserOverrideRequest request) {
        return ResponseEntity.ok(featureFlagService.setBulkUserOverrides(request));
    }

    /**
     * Remove a user override by ID
     */
    @DeleteMapping("/overrides/{overrideId}")
    public ResponseEntity<Map<String, String>> removeUserOverride(@PathVariable Long overrideId) {
        featureFlagService.removeUserOverride(overrideId);
        return ResponseEntity.ok(Map.of("message", "User override removed successfully"));
    }

    /**
     * Remove user override by user ID and feature flag ID
     */
    @DeleteMapping("/{featureFlagId}/overrides/user/{userId}")
    public ResponseEntity<Map<String, String>> removeUserOverrideByUserAndFeature(
            @PathVariable Long featureFlagId,
            @PathVariable Long userId) {
        featureFlagService.removeUserOverrideByUserAndFeature(userId, featureFlagId);
        return ResponseEntity.ok(Map.of("message", "User override removed successfully"));
    }

    // ==================== Mobile App Endpoints ====================

    /**
     * Check if a specific feature is enabled for a user
     */
    @GetMapping("/check/{featureKey}")
    public ResponseEntity<Map<String, Object>> checkFeatureForUser(
            @PathVariable String featureKey,
            @RequestParam Long userId) {
        boolean enabled = featureFlagService.isFeatureEnabledForUser(userId, featureKey);
        return ResponseEntity.ok(Map.of(
                "featureKey", featureKey,
                "userId", userId,
                "enabled", enabled
        ));
    }

    /**
     * Get all features with their status for a specific user
     * This endpoint is used by mobile app to determine which features to show
     */
    @GetMapping("/user/{userId}/features")
    public ResponseEntity<FeatureFlagDTO.UserFeaturesResponse> getUserFeatures(
            @PathVariable Long userId,
            @RequestParam(required = false) String platform) {
        return ResponseEntity.ok(featureFlagService.getUserFeatures(userId, platform));
    }
}
