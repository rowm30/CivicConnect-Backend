package com.civicconnect.api.controller;

import com.civicconnect.api.dto.RepresentativeRatingDTO.*;
import com.civicconnect.api.entity.RepresentativeRating.RepresentativeType;
import com.civicconnect.api.service.RepresentativeRatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for representative ratings and Gov Map stats
 */
@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RepresentativeRatingController {

    private final RepresentativeRatingService ratingService;

    /**
     * Submit or update a rating for a representative
     */
    @PostMapping
    public ResponseEntity<RatingResponse> submitRating(
            @RequestBody RatingRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("User {} submitting rating for {} {} - rating: {}",
                userId, request.getRepresentativeType(), request.getRepresentativeId(), request.getRating());

        RatingResponse response = ratingService.submitRating(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's rating for a specific representative
     */
    @GetMapping("/user/{userId}/representative/{type}/{representativeId}")
    public ResponseEntity<RatingResponse> getUserRating(
            @PathVariable Long userId,
            @PathVariable RepresentativeType type,
            @PathVariable Long representativeId) {

        return ratingService.getUserRating(userId, type, representativeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all ratings by a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RatingResponse>> getUserRatings(@PathVariable Long userId) {
        return ResponseEntity.ok(ratingService.getUserRatings(userId));
    }

    /**
     * Get aggregate rating stats for a representative
     */
    @GetMapping("/stats/{type}/{representativeId}")
    public ResponseEntity<RatingStats> getRatingStats(
            @PathVariable RepresentativeType type,
            @PathVariable Long representativeId) {

        return ResponseEntity.ok(ratingService.getRatingStats(type, representativeId));
    }

    /**
     * Get issue stats for a representative
     */
    @GetMapping("/issues/{type}/{representativeId}")
    public ResponseEntity<IssueStats> getIssueStats(
            @PathVariable RepresentativeType type,
            @PathVariable Long representativeId,
            @RequestParam(required = false) String representativeName) {

        return ResponseEntity.ok(ratingService.getIssueStats(type, representativeId, representativeName));
    }

    /**
     * Get combined representative stats for Gov Map (ratings + issues)
     */
    @GetMapping("/representative/{type}/{representativeId}")
    public ResponseEntity<RepresentativeStats> getRepresentativeStats(
            @PathVariable RepresentativeType type,
            @PathVariable Long representativeId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String party,
            @RequestParam(required = false) String constituency,
            @RequestParam(required = false) String photoUrl,
            @RequestParam(required = false) String designation,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        return ResponseEntity.ok(ratingService.getRepresentativeStats(
                type, representativeId, name, party, constituency, photoUrl, designation, userId));
    }

    /**
     * Get recent reviews for a representative
     */
    @GetMapping("/reviews/{type}/{representativeId}")
    public ResponseEntity<List<RatingResponse>> getRecentReviews(
            @PathVariable RepresentativeType type,
            @PathVariable Long representativeId,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(ratingService.getRecentReviews(type, representativeId, limit));
    }

    /**
     * Get top rated representatives by type
     */
    @GetMapping("/top/{type}")
    public ResponseEntity<List<RepresentativeStats>> getTopRated(
            @PathVariable RepresentativeType type,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "5") Long minRatings) {

        return ResponseEntity.ok(ratingService.getTopRated(type, limit, minRatings));
    }

    /**
     * Delete user's rating for a representative
     */
    @DeleteMapping("/{type}/{representativeId}")
    public ResponseEntity<Map<String, String>> deleteRating(
            @PathVariable RepresentativeType type,
            @PathVariable Long representativeId,
            @RequestHeader("X-User-Id") Long userId) {

        ratingService.deleteRating(userId, type, representativeId);
        return ResponseEntity.ok(Map.of("message", "Rating deleted successfully"));
    }

    /**
     * Check if user has rated a representative
     */
    @GetMapping("/check/{type}/{representativeId}")
    public ResponseEntity<Map<String, Object>> checkUserRating(
            @PathVariable RepresentativeType type,
            @PathVariable Long representativeId,
            @RequestHeader("X-User-Id") Long userId) {

        var rating = ratingService.getUserRating(userId, type, representativeId);
        return ResponseEntity.ok(Map.of(
                "hasRated", rating.isPresent(),
                "rating", rating.map(RatingResponse::getRating).orElse(null)
        ));
    }
}
