package com.civicconnect.api.repository;

import com.civicconnect.api.entity.UserFeatureOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFeatureOverrideRepository extends JpaRepository<UserFeatureOverride, Long> {

    /**
     * Find all overrides for a specific user
     */
    List<UserFeatureOverride> findByUserId(Long userId);

    /**
     * Find all overrides for a specific feature
     */
    @Query("SELECT o FROM UserFeatureOverride o WHERE o.featureFlag.id = :featureFlagId")
    List<UserFeatureOverride> findByFeatureFlagId(@Param("featureFlagId") Long featureFlagId);

    /**
     * Find override for a specific user and feature
     */
    @Query("SELECT o FROM UserFeatureOverride o WHERE o.userId = :userId AND o.featureFlag.id = :featureFlagId")
    Optional<UserFeatureOverride> findByUserIdAndFeatureFlagId(
            @Param("userId") Long userId,
            @Param("featureFlagId") Long featureFlagId);

    /**
     * Find override for a specific user and feature key
     */
    @Query("SELECT o FROM UserFeatureOverride o WHERE o.userId = :userId AND o.featureFlag.featureKey = :featureKey")
    Optional<UserFeatureOverride> findByUserIdAndFeatureKey(
            @Param("userId") Long userId,
            @Param("featureKey") String featureKey);

    /**
     * Delete all overrides for a specific feature
     */
    @Query("DELETE FROM UserFeatureOverride o WHERE o.featureFlag.id = :featureFlagId")
    void deleteByFeatureFlagId(@Param("featureFlagId") Long featureFlagId);

    /**
     * Delete all overrides for a specific user
     */
    void deleteByUserId(Long userId);

    /**
     * Count users with overrides for a specific feature
     */
    @Query("SELECT COUNT(o) FROM UserFeatureOverride o WHERE o.featureFlag.id = :featureFlagId")
    long countByFeatureFlagId(@Param("featureFlagId") Long featureFlagId);

    /**
     * Count enabled overrides for a feature
     */
    @Query("SELECT COUNT(o) FROM UserFeatureOverride o WHERE o.featureFlag.id = :featureFlagId AND o.enabled = true")
    long countEnabledByFeatureFlagId(@Param("featureFlagId") Long featureFlagId);

    /**
     * Count disabled overrides for a feature
     */
    @Query("SELECT COUNT(o) FROM UserFeatureOverride o WHERE o.featureFlag.id = :featureFlagId AND o.enabled = false")
    long countDisabledByFeatureFlagId(@Param("featureFlagId") Long featureFlagId);

    /**
     * Check if an override exists for user and feature
     */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM UserFeatureOverride o WHERE o.userId = :userId AND o.featureFlag.id = :featureFlagId")
    boolean existsByUserIdAndFeatureFlagId(@Param("userId") Long userId, @Param("featureFlagId") Long featureFlagId);
}
