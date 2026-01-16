package com.civicconnect.api.repository;

import com.civicconnect.api.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {

    /**
     * Find a feature flag by its unique key
     */
    Optional<FeatureFlag> findByFeatureKey(String featureKey);

    /**
     * Find all feature flags ordered by category and display order
     */
    List<FeatureFlag> findAllByOrderByCategoryAscDisplayOrderAsc();

    /**
     * Find all active feature flags
     */
    List<FeatureFlag> findByIsActiveTrueOrderByCategoryAscDisplayOrderAsc();

    /**
     * Find feature flags by category
     */
    List<FeatureFlag> findByCategoryOrderByDisplayOrderAsc(String category);

    /**
     * Find all globally enabled features
     */
    List<FeatureFlag> findByEnabledGloballyTrueAndIsActiveTrue();

    /**
     * Find features by platform
     */
    @Query("SELECT f FROM FeatureFlag f WHERE f.isActive = true AND (f.platform = :platform OR f.platform = 'ALL')")
    List<FeatureFlag> findByPlatform(@Param("platform") String platform);

    /**
     * Check if a feature key already exists
     */
    boolean existsByFeatureKey(String featureKey);

    /**
     * Find all distinct categories
     */
    @Query("SELECT DISTINCT f.category FROM FeatureFlag f WHERE f.category IS NOT NULL ORDER BY f.category")
    List<String> findAllCategories();
}
