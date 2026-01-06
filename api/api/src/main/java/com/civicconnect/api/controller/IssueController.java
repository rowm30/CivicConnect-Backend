package com.civicconnect.api.controller;

import com.civicconnect.api.dto.CreateIssueRequest;
import com.civicconnect.api.dto.IssueDTO;
import com.civicconnect.api.dto.VoteRequest;
import com.civicconnect.api.service.IssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Issue Controller for Issue Pulse feature
 * Handles issue CRUD, voting, and heat score queries
 */
@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class IssueController {

    private final IssueService issueService;

    /**
     * Get hottest issues (sorted by heat score)
     * Used by Issue Pulse tab
     */
    @GetMapping
    public ResponseEntity<Page<IssueDTO>> getHottestIssues(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        log.info("Getting hottest issues - page: {}, size: {}", page, size);
        Page<IssueDTO> issues = issueService.getHottestIssues(page, size, userId);
        return ResponseEntity.ok(issues);
    }

    /**
     * Get issues by category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<IssueDTO>> getIssuesByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        log.info("Getting issues by category: {}", category);
        Page<IssueDTO> issues = issueService.getIssuesByCategory(category, page, size, userId);
        return ResponseEntity.ok(issues);
    }

    /**
     * Get issues by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<IssueDTO>> getIssuesByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        log.info("Getting issues by status: {}", status);
        Page<IssueDTO> issues = issueService.getIssuesByStatus(status, page, size, userId);
        return ResponseEntity.ok(issues);
    }

    /**
     * Get nearby issues
     */
    @GetMapping("/nearby")
    public ResponseEntity<Page<IssueDTO>> getNearbyIssues(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "5") Double radiusKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        log.info("Getting nearby issues - lat: {}, lng: {}, radius: {}km", lat, lng, radiusKm);
        Page<IssueDTO> issues = issueService.getNearbyIssues(lat, lng, radiusKm, page, size, userId);
        return ResponseEntity.ok(issues);
    }

    /**
     * Search issues
     */
    @GetMapping("/search")
    public ResponseEntity<Page<IssueDTO>> searchIssues(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        log.info("Searching issues: {}", q);
        Page<IssueDTO> issues = issueService.searchIssues(q, page, size, userId);
        return ResponseEntity.ok(issues);
    }

    /**
     * Get issues by parliamentary constituency
     * Primary filter for Issue Pulse - shows issues in user's constituency
     */
    @GetMapping("/constituency/pc/{constituency}")
    public ResponseEntity<Page<IssueDTO>> getIssuesByParliamentaryConstituency(
            @PathVariable String constituency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        log.info("Getting issues by parliamentary constituency: {}", constituency);
        Page<IssueDTO> issues = issueService.getIssuesByParliamentaryConstituency(constituency, page, size, userId);
        return ResponseEntity.ok(issues);
    }

    /**
     * Get issues by assembly constituency
     */
    @GetMapping("/constituency/ac/{constituency}")
    public ResponseEntity<Page<IssueDTO>> getIssuesByAssemblyConstituency(
            @PathVariable String constituency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        log.info("Getting issues by assembly constituency: {}", constituency);
        Page<IssueDTO> issues = issueService.getIssuesByAssemblyConstituency(constituency, page, size, userId);
        return ResponseEntity.ok(issues);
    }

    /**
     * Get issues by constituency (matches either PC or AC)
     * Used when user wants to see all issues in their area
     */
    @GetMapping("/constituency")
    public ResponseEntity<Page<IssueDTO>> getIssuesByConstituency(
            @RequestParam(required = false) String pc,
            @RequestParam(required = false) String ac,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        log.info("Getting issues by constituency - PC: {}, AC: {}", pc, ac);
        Page<IssueDTO> issues = issueService.getIssuesByConstituency(pc, ac, page, size, userId);
        return ResponseEntity.ok(issues);
    }

    /**
     * Get issue by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<IssueDTO> getIssueById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        log.info("Getting issue by ID: {}", id);
        IssueDTO issue = issueService.getIssueById(id, userId);
        return ResponseEntity.ok(issue);
    }

    /**
     * Get issue by tracking ID
     */
    @GetMapping("/track/{trackingId}")
    public ResponseEntity<IssueDTO> getIssueByTrackingId(
            @PathVariable String trackingId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        log.info("Getting issue by tracking ID: {}", trackingId);
        IssueDTO issue = issueService.getIssueByTrackingId(trackingId, userId);
        return ResponseEntity.ok(issue);
    }

    /**
     * Get issues reported by user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<IssueDTO>> getIssuesByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Getting issues by user: {}", userId);
        Page<IssueDTO> issues = issueService.getIssuesByReporter(userId, page, size);
        return ResponseEntity.ok(issues);
    }

    /**
     * Create a new issue
     */
    @PostMapping
    public ResponseEntity<IssueDTO> createIssue(
            @Valid @RequestBody CreateIssueRequest request,
            @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("Creating issue: {} by user: {}", request.getTitle(), userId);
        IssueDTO issue = issueService.createIssue(request, userId);
        return ResponseEntity.ok(issue);
    }

    /**
     * Vote on an issue (upvote or downvote)
     */
    @PostMapping("/{id}/vote")
    public ResponseEntity<IssueDTO> vote(
            @PathVariable Long id,
            @Valid @RequestBody VoteRequest request,
            @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("User {} voting {} on issue {}", userId, request.getVoteType(), id);
        IssueDTO issue = issueService.vote(id, userId, request.getVoteType());
        return ResponseEntity.ok(issue);
    }

    /**
     * Remove vote from an issue
     */
    @DeleteMapping("/{id}/vote")
    public ResponseEntity<IssueDTO> removeVote(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("User {} removing vote from issue {}", userId, id);
        IssueDTO issue = issueService.removeVote(id, userId);
        return ResponseEntity.ok(issue);
    }

    /**
     * Update issue status (admin/official only)
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<IssueDTO> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String status = body.get("status");
        log.info("Updating issue {} status to: {}", id, status);
        IssueDTO issue = issueService.updateStatus(id, status);
        return ResponseEntity.ok(issue);
    }

    /**
     * Assign issue to official
     */
    @PatchMapping("/{id}/assign")
    public ResponseEntity<IssueDTO> assignToOfficial(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        Long officialId = ((Number) body.get("officialId")).longValue();
        String officialName = (String) body.get("officialName");
        log.info("Assigning issue {} to official: {}", id, officialName);
        IssueDTO issue = issueService.assignToOfficial(id, officialId, officialName);
        return ResponseEntity.ok(issue);
    }

    /**
     * Get user's issue stats for profile
     */
    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<IssueService.UserIssueStats> getUserStats(@PathVariable Long userId) {
        log.info("Getting issue stats for user: {}", userId);
        IssueService.UserIssueStats stats = issueService.getUserStats(userId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Backfill MLA/MP/Councillor data for existing issues
     * Updates all issues that have location but missing representative info
     */
    @PostMapping("/backfill-representatives")
    public ResponseEntity<Map<String, Object>> backfillRepresentatives() {
        log.info("Backfilling MLA/MP/Councillor data for existing issues");
        Map<String, Object> result = issueService.backfillRepresentativeData();
        return ResponseEntity.ok(result);
    }
}
