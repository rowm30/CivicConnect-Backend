package com.civicconnect.api.controller;

import com.civicconnect.api.dto.GovMapDTO.*;
import com.civicconnect.api.service.GovMapHierarchyService;
import com.civicconnect.api.service.GovNodeInfoSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API Controller for Government Map Hierarchy
 *
 * Provides fixed hierarchy structure for India's democratic system:
 * - LOCAL: Citizen → Ward Councillor → Mayor
 * - STATE: Citizen → MLA → Chief Minister
 * - NATIONAL: Citizen → MP → Prime Minister
 */
@RestController
@RequestMapping("/api/gov-map")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Government Map", description = "APIs for government hierarchy visualization")
public class GovMapController {

    private final GovMapHierarchyService govMapHierarchyService;
    private final GovNodeInfoSubmissionService submissionService;

    /**
     * Get complete government map with all three hierarchies
     */
    @GetMapping("/complete")
    @Operation(summary = "Get complete GovMap", description = "Returns all three hierarchy levels (Local, State, National) for the user's location")
    public ResponseEntity<CompleteGovMapResponse> getCompleteGovMap(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String stateCode,
            @RequestParam(required = false) String stateName,
            @RequestParam(required = false) String cityName,
            @RequestParam(required = false) Long assemblyConstituencyId,
            @RequestParam(required = false) Long parliamentaryConstituencyId
    ) {
        log.info("Getting complete GovMap for user: {}, location: ({}, {}), state: {}, city: {}",
                userId, latitude, longitude, stateName, cityName);

        CompleteGovMapResponse response = govMapHierarchyService.getCompleteGovMap(
                userId, latitude, longitude, stateCode, stateName, cityName,
                assemblyConstituencyId, parliamentaryConstituencyId
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get LOCAL hierarchy: Citizen → Ward Councillor → Mayor
     */
    @GetMapping("/hierarchy/local")
    @Operation(summary = "Get Local hierarchy", description = "Returns municipal/local government hierarchy")
    public ResponseEntity<FixedHierarchyResponse> getLocalHierarchy(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String stateName,
            @RequestParam(required = false) String cityName
    ) {
        log.info("Getting LOCAL hierarchy for city: {}, state: {}", cityName, stateName);

        FixedHierarchyResponse response = govMapHierarchyService.buildLocalHierarchy(
                userId, stateName, cityName, latitude, longitude
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get STATE hierarchy: Citizen → MLA → Chief Minister
     */
    @GetMapping("/hierarchy/state")
    @Operation(summary = "Get State hierarchy", description = "Returns state government hierarchy")
    public ResponseEntity<FixedHierarchyResponse> getStateHierarchy(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(required = false) String stateCode,
            @RequestParam(required = false) String stateName,
            @RequestParam(required = false) Long assemblyConstituencyId
    ) {
        log.info("Getting STATE hierarchy for state: {} (code: {}), AC: {}", stateName, stateCode, assemblyConstituencyId);

        FixedHierarchyResponse response = govMapHierarchyService.buildStateHierarchy(
                userId, stateCode, stateName, assemblyConstituencyId
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get NATIONAL hierarchy: Citizen → MP → Prime Minister
     */
    @GetMapping("/hierarchy/national")
    @Operation(summary = "Get National hierarchy", description = "Returns national government hierarchy")
    public ResponseEntity<FixedHierarchyResponse> getNationalHierarchy(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(required = false) String stateCode,
            @RequestParam(required = false) String stateName,
            @RequestParam(required = false) Long parliamentaryConstituencyId
    ) {
        log.info("Getting NATIONAL hierarchy for state: {}, PC: {}", stateName, parliamentaryConstituencyId);

        FixedHierarchyResponse response = govMapHierarchyService.buildNationalHierarchy(
                userId, stateCode, stateName, parliamentaryConstituencyId
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get hierarchy structure definitions (for frontend to understand the fixed structure)
     */
    @GetMapping("/structure")
    @Operation(summary = "Get hierarchy structure definitions", description = "Returns the fixed structure definitions for all hierarchy modes")
    public ResponseEntity<HierarchyStructureResponse> getHierarchyStructure() {
        HierarchyStructureResponse response = HierarchyStructureResponse.builder()
                .local(HierarchyDefinitions.getLocalStructure())
                .state(HierarchyDefinitions.getStateStructure())
                .national(HierarchyDefinitions.getNationalStructure())
                .build();

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // Node Info Submission Endpoints (User-contributed data)
    // =====================================================

    /**
     * Submit information about a government node where data is not available
     */
    @PostMapping("/submit-info")
    @Operation(summary = "Submit node info", description = "Submit information about a government official for nodes without data")
    public ResponseEntity<NodeInfoSubmissionResponse> submitNodeInfo(
            @Valid @RequestBody NodeInfoSubmissionRequest request,
            @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("User {} submitting info for {} - {}", userId, request.getNodeType(), request.getOfficialName());
        NodeInfoSubmissionResponse response = submissionService.submitNodeInfo(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's submissions
     */
    @GetMapping("/submissions/my")
    @Operation(summary = "Get my submissions", description = "Get list of node info submissions by current user")
    public ResponseEntity<UserSubmissionsResponse> getMySubmissions(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Getting submissions for user {}", userId);
        UserSubmissionsResponse response = submissionService.getUserSubmissions(userId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get submission by ID
     */
    @GetMapping("/submissions/{id}")
    @Operation(summary = "Get submission", description = "Get details of a specific submission")
    public ResponseEntity<SubmissionSummary> getSubmission(@PathVariable Long id) {
        SubmissionSummary submission = submissionService.getSubmission(id);
        return ResponseEntity.ok(submission);
    }

    /**
     * Get pending submissions (admin only)
     */
    @GetMapping("/submissions/pending")
    @Operation(summary = "Get pending submissions", description = "Get all pending submissions for admin review")
    public ResponseEntity<Page<SubmissionSummary>> getPendingSubmissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<SubmissionSummary> submissions = submissionService.getPendingSubmissions(page, size);
        return ResponseEntity.ok(submissions);
    }

    /**
     * Review a submission (admin only)
     */
    @PatchMapping("/submissions/{id}/review")
    @Operation(summary = "Review submission", description = "Approve or reject a node info submission")
    public ResponseEntity<SubmissionSummary> reviewSubmission(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String reviewNotes,
            @RequestHeader("X-User-Id") Long reviewerId
    ) {
        log.info("Reviewer {} reviewing submission {} with status {}", reviewerId, id, status);
        SubmissionSummary result = submissionService.reviewSubmission(id, status, reviewNotes, reviewerId);
        return ResponseEntity.ok(result);
    }

    /**
     * Response containing all hierarchy structure definitions
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HierarchyStructureResponse {
        private HierarchyStructure local;
        private HierarchyStructure state;
        private HierarchyStructure national;
    }
}
