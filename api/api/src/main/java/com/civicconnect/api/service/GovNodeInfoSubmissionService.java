package com.civicconnect.api.service;

import com.civicconnect.api.dto.GovMapDTO.*;
import com.civicconnect.api.entity.GovNodeInfoSubmission;
import com.civicconnect.api.entity.GovNodeInfoSubmission.SubmissionStatus;
import com.civicconnect.api.entity.analytics.AppUser;
import com.civicconnect.api.repository.GovNodeInfoSubmissionRepository;
import com.civicconnect.api.repository.analytics.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GovNodeInfoSubmissionService {

    private final GovNodeInfoSubmissionRepository submissionRepository;
    private final AppUserRepository appUserRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Submit information about a government node
     */
    @Transactional
    public NodeInfoSubmissionResponse submitNodeInfo(NodeInfoSubmissionRequest request, Long userId) {
        // Validate user
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Check for potential duplicates
        List<GovNodeInfoSubmission> duplicates = submissionRepository.findPotentialDuplicates(
                request.getNodeType(),
                request.getStateName(),
                request.getOfficialName()
        );

        if (!duplicates.isEmpty()) {
            log.warn("Potential duplicate submission found for {} - {}",
                    request.getNodeType(), request.getOfficialName());
        }

        // Create submission entity
        GovNodeInfoSubmission submission = GovNodeInfoSubmission.builder()
                .nodeType(request.getNodeType())
                .hierarchyMode(request.getHierarchyMode())
                .stateName(request.getStateName())
                .stateCode(request.getStateCode())
                .cityName(request.getCityName())
                .districtName(request.getDistrictName())
                .wardName(request.getWardName())
                .wardNumber(request.getWardNumber())
                .assemblyConstituency(request.getAssemblyConstituency())
                .parliamentaryConstituency(request.getParliamentaryConstituency())
                .officialName(request.getOfficialName())
                .party(request.getParty())
                .partyAbbreviation(request.getPartyAbbreviation())
                .designation(request.getDesignation())
                .photoUrl(request.getPhotoUrl())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .twitterHandle(request.getTwitterHandle())
                .additionalInfo(request.getAdditionalInfo())
                .sourceUrl(request.getSourceUrl())
                .sourceDescription(request.getSourceDescription())
                .submittedBy(user)
                .status(SubmissionStatus.PENDING)
                .build();

        GovNodeInfoSubmission saved = submissionRepository.save(submission);
        log.info("Node info submission created: ID={}, type={}, official={}, by user={}",
                saved.getId(), saved.getNodeType(), saved.getOfficialName(), userId);

        return NodeInfoSubmissionResponse.builder()
                .submissionId(saved.getId())
                .status(saved.getStatus().name())
                .message("Thank you! Your submission is under review.")
                .nodeType(saved.getNodeType())
                .officialName(saved.getOfficialName())
                .submittedAt(saved.getCreatedAt().format(DATE_FORMATTER))
                .build();
    }

    /**
     * Get user's submissions
     */
    @Transactional(readOnly = true)
    public UserSubmissionsResponse getUserSubmissions(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<GovNodeInfoSubmission> submissions = submissionRepository.findBySubmittedById(userId, pageable);

        // Count by status
        long pending = submissions.getContent().stream()
                .filter(s -> s.getStatus() == SubmissionStatus.PENDING).count();
        long approved = submissions.getContent().stream()
                .filter(s -> s.getStatus() == SubmissionStatus.APPROVED).count();
        long rejected = submissions.getContent().stream()
                .filter(s -> s.getStatus() == SubmissionStatus.REJECTED).count();

        List<SubmissionSummary> summaries = submissions.getContent().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        return UserSubmissionsResponse.builder()
                .totalSubmissions(submissions.getTotalElements())
                .pendingCount(pending)
                .approvedCount(approved)
                .rejectedCount(rejected)
                .submissions(summaries)
                .build();
    }

    /**
     * Get submission by ID
     */
    @Transactional(readOnly = true)
    public SubmissionSummary getSubmission(Long submissionId) {
        GovNodeInfoSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));
        return toSummary(submission);
    }

    /**
     * Get all pending submissions (for admin review)
     */
    @Transactional(readOnly = true)
    public Page<SubmissionSummary> getPendingSubmissions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return submissionRepository.findByStatus(SubmissionStatus.PENDING, pageable)
                .map(this::toSummary);
    }

    /**
     * Review a submission (approve/reject)
     */
    @Transactional
    public SubmissionSummary reviewSubmission(Long submissionId, String status, String reviewNotes, Long reviewerId) {
        GovNodeInfoSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));

        SubmissionStatus newStatus = SubmissionStatus.valueOf(status.toUpperCase());
        submission.setStatus(newStatus);
        submission.setReviewNotes(reviewNotes);
        submission.setReviewedById(reviewerId);
        submission.setReviewedAt(java.time.LocalDateTime.now());

        GovNodeInfoSubmission updated = submissionRepository.save(submission);
        log.info("Submission {} reviewed: status={}, by reviewer={}", submissionId, newStatus, reviewerId);

        return toSummary(updated);
    }

    private SubmissionSummary toSummary(GovNodeInfoSubmission submission) {
        return SubmissionSummary.builder()
                .id(submission.getId())
                .nodeType(submission.getNodeType())
                .hierarchyMode(submission.getHierarchyMode())
                .officialName(submission.getOfficialName())
                .stateName(submission.getStateName())
                .status(submission.getStatus().name())
                .submittedAt(submission.getCreatedAt() != null ?
                        submission.getCreatedAt().format(DATE_FORMATTER) : null)
                .reviewNotes(submission.getReviewNotes())
                .build();
    }
}
