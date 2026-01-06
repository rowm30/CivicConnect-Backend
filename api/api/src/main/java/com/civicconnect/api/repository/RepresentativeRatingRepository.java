package com.civicconnect.api.repository;

import com.civicconnect.api.entity.RepresentativeRating;
import com.civicconnect.api.entity.RepresentativeRating.RepresentativeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for representative ratings
 */
@Repository
public interface RepresentativeRatingRepository extends JpaRepository<RepresentativeRating, Long> {

    /**
     * Find user's rating for a specific representative
     */
    Optional<RepresentativeRating> findByUserIdAndRepresentativeTypeAndRepresentativeId(
            Long userId, RepresentativeType type, Long representativeId);

    /**
     * Get all ratings for a representative
     */
    List<RepresentativeRating> findByRepresentativeTypeAndRepresentativeIdOrderByCreatedAtDesc(
            RepresentativeType type, Long representativeId);

    /**
     * Get all ratings by a user
     */
    List<RepresentativeRating> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Get average rating for a representative
     */
    @Query("SELECT AVG(r.rating) FROM RepresentativeRating r " +
           "WHERE r.representativeType = :type AND r.representativeId = :repId")
    Double getAverageRating(@Param("type") RepresentativeType type, @Param("repId") Long representativeId);

    /**
     * Get rating count for a representative
     */
    @Query("SELECT COUNT(r) FROM RepresentativeRating r " +
           "WHERE r.representativeType = :type AND r.representativeId = :repId")
    Long getRatingCount(@Param("type") RepresentativeType type, @Param("repId") Long representativeId);

    /**
     * Get rating distribution (count per star) for a representative
     */
    @Query("SELECT r.rating, COUNT(r) FROM RepresentativeRating r " +
           "WHERE r.representativeType = :type AND r.representativeId = :repId " +
           "GROUP BY r.rating ORDER BY r.rating")
    List<Object[]> getRatingDistribution(@Param("type") RepresentativeType type, @Param("repId") Long representativeId);

    /**
     * Get aggregate stats for a representative
     * Returns: [averageRating, totalCount, fiveStarCount, fourStarCount, threeStarCount, twoStarCount, oneStarCount]
     */
    @Query("SELECT " +
           "COALESCE(AVG(r.rating), 0), " +
           "COUNT(r), " +
           "SUM(CASE WHEN r.rating = 5 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN r.rating = 4 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN r.rating = 3 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN r.rating = 2 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN r.rating = 1 THEN 1 ELSE 0 END) " +
           "FROM RepresentativeRating r " +
           "WHERE r.representativeType = :type AND r.representativeId = :repId")
    Object[] getAggregateStats(@Param("type") RepresentativeType type, @Param("repId") Long representativeId);

    /**
     * Get recent ratings with comments for a representative (for display)
     */
    @Query("SELECT r FROM RepresentativeRating r " +
           "WHERE r.representativeType = :type AND r.representativeId = :repId " +
           "AND r.comment IS NOT NULL AND r.comment <> '' " +
           "ORDER BY r.createdAt DESC")
    List<RepresentativeRating> getRecentReviews(@Param("type") RepresentativeType type,
                                                 @Param("repId") Long representativeId);

    /**
     * Count verified ratings for a representative
     */
    @Query("SELECT COUNT(r) FROM RepresentativeRating r " +
           "WHERE r.representativeType = :type AND r.representativeId = :repId AND r.isVerified = true")
    Long getVerifiedRatingCount(@Param("type") RepresentativeType type, @Param("repId") Long representativeId);

    /**
     * Get top rated representatives by type
     */
    @Query("SELECT r.representativeId, r.representativeName, r.partyName, r.constituencyName, " +
           "AVG(r.rating) as avgRating, COUNT(r) as ratingCount " +
           "FROM RepresentativeRating r " +
           "WHERE r.representativeType = :type " +
           "GROUP BY r.representativeId, r.representativeName, r.partyName, r.constituencyName " +
           "HAVING COUNT(r) >= :minRatings " +
           "ORDER BY AVG(r.rating) DESC")
    List<Object[]> getTopRatedByType(@Param("type") RepresentativeType type, @Param("minRatings") Long minRatings);

    /**
     * Get lowest rated representatives by type
     */
    @Query("SELECT r.representativeId, r.representativeName, r.partyName, r.constituencyName, " +
           "AVG(r.rating) as avgRating, COUNT(r) as ratingCount " +
           "FROM RepresentativeRating r " +
           "WHERE r.representativeType = :type " +
           "GROUP BY r.representativeId, r.representativeName, r.partyName, r.constituencyName " +
           "HAVING COUNT(r) >= :minRatings " +
           "ORDER BY AVG(r.rating) ASC")
    List<Object[]> getLowestRatedByType(@Param("type") RepresentativeType type, @Param("minRatings") Long minRatings);

    /**
     * Check if user has already rated this representative
     */
    boolean existsByUserIdAndRepresentativeTypeAndRepresentativeId(
            Long userId, RepresentativeType type, Long representativeId);

    /**
     * Delete user's rating
     */
    void deleteByUserIdAndRepresentativeTypeAndRepresentativeId(
            Long userId, RepresentativeType type, Long representativeId);
}
