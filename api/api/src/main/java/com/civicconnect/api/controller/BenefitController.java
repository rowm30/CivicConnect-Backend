package com.civicconnect.api.controller;

import com.civicconnect.api.dto.BenefitDTO;
import com.civicconnect.api.service.BenefitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Benefit Controller for Benefit Check feature
 * Handles benefit queries, eligibility checks, and user applications
 */
@RestController
@RequestMapping("/api/benefits")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BenefitController {

    private final BenefitService benefitService;

    /**
     * Get all active benefits
     */
    @GetMapping
    public ResponseEntity<Page<BenefitDTO>> getAllBenefits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        log.info("Getting all benefits - page: {}, size: {}", page, size);
        Page<BenefitDTO> benefits = benefitService.getAllBenefits(page, size, userId);
        return ResponseEntity.ok(benefits);
    }

    /**
     * Get benefits by category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<BenefitDTO>> getBenefitsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        log.info("Getting benefits by category: {}", category);
        Page<BenefitDTO> benefits = benefitService.getBenefitsByCategory(category, page, size, userId);
        return ResponseEntity.ok(benefits);
    }

    /**
     * Get benefits by government level
     */
    @GetMapping("/level/{level}")
    public ResponseEntity<Page<BenefitDTO>> getBenefitsByLevel(
            @PathVariable String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        log.info("Getting benefits by level: {}", level);
        Page<BenefitDTO> benefits = benefitService.getBenefitsByLevel(level, page, size, userId);
        return ResponseEntity.ok(benefits);
    }

    /**
     * Get eligible benefits based on user profile
     * This is the main eligibility check endpoint
     */
    @GetMapping("/eligible")
    public ResponseEntity<Page<BenefitDTO>> getEligibleBenefits(
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) BigDecimal income,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        log.info("Getting eligible benefits - age: {}, income: {}, gender: {}, state: {}", age, income, gender, state);
        Page<BenefitDTO> benefits = benefitService.getEligibleBenefits(age, income, gender, state, page, size, userId);
        return ResponseEntity.ok(benefits);
    }

    /**
     * Search benefits
     */
    @GetMapping("/search")
    public ResponseEntity<Page<BenefitDTO>> searchBenefits(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        log.info("Searching benefits: {}", q);
        Page<BenefitDTO> benefits = benefitService.searchBenefits(q, page, size, userId);
        return ResponseEntity.ok(benefits);
    }

    /**
     * Get benefit by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BenefitDTO> getBenefitById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        log.info("Getting benefit by ID: {}", id);
        BenefitDTO benefit = benefitService.getBenefitById(id, userId);
        return ResponseEntity.ok(benefit);
    }

    /**
     * Get popular benefits
     */
    @GetMapping("/popular")
    public ResponseEntity<Page<BenefitDTO>> getPopularBenefits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        log.info("Getting popular benefits");
        Page<BenefitDTO> benefits = benefitService.getPopularBenefits(page, size, userId);
        return ResponseEntity.ok(benefits);
    }

    /**
     * Get category counts for filtering UI
     */
    @GetMapping("/categories/counts")
    public ResponseEntity<Map<String, Long>> getCategoryCounts() {
        log.info("Getting category counts");
        Map<String, Long> counts = benefitService.getCategoryCounts();
        return ResponseEntity.ok(counts);
    }

    /**
     * Save benefit for later
     */
    @PostMapping("/{id}/save")
    public ResponseEntity<BenefitDTO> saveBenefit(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("User {} saving benefit {}", userId, id);
        BenefitDTO benefit = benefitService.saveBenefit(id, userId);
        return ResponseEntity.ok(benefit);
    }

    /**
     * Remove saved benefit
     */
    @DeleteMapping("/{id}/save")
    public ResponseEntity<Void> removeSavedBenefit(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("User {} removing saved benefit {}", userId, id);
        benefitService.removeSavedBenefit(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Mark benefit as applied
     */
    @PostMapping("/{id}/apply")
    public ResponseEntity<BenefitDTO> markAsApplied(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader("X-User-Id") Long userId
    ) {
        String reference = body != null ? body.get("applicationReference") : null;
        log.info("User {} applying for benefit {}", userId, id);
        BenefitDTO benefit = benefitService.markAsApplied(id, userId, reference);
        return ResponseEntity.ok(benefit);
    }

    /**
     * Get user's saved benefits
     */
    @GetMapping("/user/saved")
    public ResponseEntity<Page<BenefitDTO>> getSavedBenefits(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Getting saved benefits for user: {}", userId);
        Page<BenefitDTO> benefits = benefitService.getSavedBenefits(userId, page, size);
        return ResponseEntity.ok(benefits);
    }

    /**
     * Get user's applied benefits
     */
    @GetMapping("/user/applied")
    public ResponseEntity<Page<BenefitDTO>> getAppliedBenefits(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Getting applied benefits for user: {}", userId);
        Page<BenefitDTO> benefits = benefitService.getAppliedBenefits(userId, page, size);
        return ResponseEntity.ok(benefits);
    }

    /**
     * Get user's benefit stats
     */
    @GetMapping("/user/stats")
    public ResponseEntity<BenefitService.UserBenefitStats> getUserBenefitStats(
            @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("Getting benefit stats for user: {}", userId);
        BenefitService.UserBenefitStats stats = benefitService.getUserBenefitStats(userId);
        return ResponseEntity.ok(stats);
    }
}
