package com.civicconnect.api.service;

import com.civicconnect.api.dto.CreateIssueRequest;
import com.civicconnect.api.dto.IssueDTO;
import com.civicconnect.api.entity.AssemblyConstituency;
import com.civicconnect.api.entity.Issue;
import com.civicconnect.api.entity.IssueVote;
import com.civicconnect.api.entity.MemberOfLegislativeAssembly;
import com.civicconnect.api.entity.MemberOfParliament;
import com.civicconnect.api.entity.ParliamentaryConstituency;
import com.civicconnect.api.entity.analytics.AppUser;
import com.civicconnect.api.repository.AssemblyConstituencyRepository;
import com.civicconnect.api.repository.IssueRepository;
import com.civicconnect.api.repository.IssueVoteRepository;
import com.civicconnect.api.repository.MemberOfLegislativeAssemblyRepository;
import com.civicconnect.api.repository.MemberOfParliamentRepository;
import com.civicconnect.api.repository.ParliamentaryConstituencyRepository;
import com.civicconnect.api.repository.analytics.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueService {

    private final IssueRepository issueRepository;
    private final IssueVoteRepository issueVoteRepository;
    private final AppUserRepository appUserRepository;
    private final AssemblyConstituencyRepository acRepository;
    private final MemberOfLegislativeAssemblyRepository mlaRepository;
    private final ParliamentaryConstituencyRepository pcRepository;
    private final MemberOfParliamentRepository mpRepository;

    /**
     * Get hottest issues (sorted by heat score)
     */
    @Transactional(readOnly = true)
    public Page<IssueDTO> getHottestIssues(int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Issue> issues = issueRepository.findHottestIssues(pageable);
        return issues.map(issue -> mapToDTO(issue, userId));
    }

    /**
     * Get issues by category
     */
    @Transactional(readOnly = true)
    public Page<IssueDTO> getIssuesByCategory(String category, int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Issue.IssueCategory cat = Issue.IssueCategory.valueOf(category.toUpperCase());
        Page<Issue> issues = issueRepository.findByCategoryOrderByHeatScore(cat, pageable);
        return issues.map(issue -> mapToDTO(issue, userId));
    }

    /**
     * Get issues by status
     */
    @Transactional(readOnly = true)
    public Page<IssueDTO> getIssuesByStatus(String status, int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Issue.IssueStatus st = Issue.IssueStatus.valueOf(status.toUpperCase());
        Page<Issue> issues = issueRepository.findByStatusOrderByHeatScore(st, pageable);
        return issues.map(issue -> mapToDTO(issue, userId));
    }

    /**
     * Get nearby issues
     */
    @Transactional(readOnly = true)
    public Page<IssueDTO> getNearbyIssues(Double lat, Double lng, Double radiusKm, int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        // Approximate 1 degree = 111 km
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        Page<Issue> issues = issueRepository.findNearbyIssues(
                lat - latDelta, lat + latDelta,
                lng - lngDelta, lng + lngDelta,
                pageable
        );
        return issues.map(issue -> mapToDTO(issue, userId));
    }

    /**
     * Search issues
     */
    @Transactional(readOnly = true)
    public Page<IssueDTO> searchIssues(String query, int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Issue> issues = issueRepository.searchIssues(query, pageable);
        return issues.map(issue -> mapToDTO(issue, userId));
    }

    /**
     * Get issues by parliamentary constituency (primary filter for Issue Pulse)
     */
    @Transactional(readOnly = true)
    public Page<IssueDTO> getIssuesByParliamentaryConstituency(String constituency, int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Issue> issues = issueRepository.findByParliamentaryConstituencyOrderByHeatScore(constituency, pageable);
        return issues.map(issue -> mapToDTO(issue, userId));
    }

    /**
     * Get issues by assembly constituency
     */
    @Transactional(readOnly = true)
    public Page<IssueDTO> getIssuesByAssemblyConstituency(String constituency, int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Issue> issues = issueRepository.findByAssemblyConstituencyOrderByHeatScore(constituency, pageable);
        return issues.map(issue -> mapToDTO(issue, userId));
    }

    /**
     * Get issues by both parliamentary and assembly constituency
     * Returns issues that match either constituency
     */
    @Transactional(readOnly = true)
    public Page<IssueDTO> getIssuesByConstituency(String parliamentaryConstituency, String assemblyConstituency, int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Issue> issues = issueRepository.findByConstituencyOrderByHeatScore(parliamentaryConstituency, assemblyConstituency, pageable);
        return issues.map(issue -> mapToDTO(issue, userId));
    }

    /**
     * Get issue by ID
     */
    @Transactional(readOnly = true)
    public IssueDTO getIssueById(Long id, Long userId) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Issue not found: " + id));
        return mapToDTO(issue, userId);
    }

    /**
     * Get issue by tracking ID
     */
    @Transactional(readOnly = true)
    public IssueDTO getIssueByTrackingId(String trackingId, Long userId) {
        Issue issue = issueRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new RuntimeException("Issue not found with tracking ID: " + trackingId));
        return mapToDTO(issue, userId);
    }

    /**
     * Get issues by reporter
     */
    @Transactional(readOnly = true)
    public Page<IssueDTO> getIssuesByReporter(Long reporterId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Issue> issues = issueRepository.findByReporterId(reporterId, pageable);
        return issues.map(issue -> mapToDTO(issue, reporterId));
    }

    /**
     * Create a new issue
     */
    @Transactional
    public IssueDTO createIssue(CreateIssueRequest request, Long userId) {
        AppUser reporter = appUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Issue issue = new Issue();
        issue.setTitle(request.getTitle());
        issue.setDescription(request.getDescription());
        issue.setImageUrl(request.getImageUrl());
        issue.setCategory(Issue.IssueCategory.valueOf(request.getCategory().toUpperCase()));

        if (request.getPriority() != null) {
            issue.setPriority(Issue.IssuePriority.valueOf(request.getPriority().toUpperCase()));
        }

        issue.setLatitude(request.getLatitude());
        issue.setLongitude(request.getLongitude());
        issue.setLocationName(request.getLocationName());
        issue.setDistrictName(request.getDistrictName());
        issue.setStateName(request.getStateName());
        issue.setParliamentaryConstituency(request.getParliamentaryConstituency());
        issue.setAssemblyConstituency(request.getAssemblyConstituency());
        issue.setDepartmentName(request.getDepartmentName());
        issue.setReporter(reporter);

        // Auto-populate MLA and MP based on location
        if (issue.getLatitude() != null && issue.getLongitude() != null) {
            populateMlaAndMpFromLocation(issue);
        }

        // Generate tracking ID
        issue.setTrackingId(generateTrackingId());

        Issue saved = issueRepository.save(issue);
        log.info("Created issue {} with tracking ID {} - MLA: {}, MP: {}",
                saved.getId(), saved.getTrackingId(), saved.getMlaName(), saved.getMpName());

        return mapToDTO(saved, userId);
    }

    /**
     * Auto-populate MLA and MP info based on issue location
     */
    private void populateMlaAndMpFromLocation(Issue issue) {
        Double lat = issue.getLatitude();
        Double lng = issue.getLongitude();

        try {
            // Find Assembly Constituency and MLA
            Optional<AssemblyConstituency> acOptional = acRepository.findByPoint(lat, lng);
            if (acOptional.isPresent()) {
                AssemblyConstituency ac = acOptional.get();
                issue.setAssemblyConstituency(ac.getAcName());

                // Find MLA for this constituency
                Optional<MemberOfLegislativeAssembly> mlaOptional = mlaRepository.findByAssemblyConstituencyId(ac.getId());
                if (mlaOptional.isEmpty()) {
                    // Try by AC number and state
                    mlaOptional = mlaRepository.findByAcNoAndStateName(ac.getAcNo(), ac.getStateName());
                }
                if (mlaOptional.isPresent()) {
                    MemberOfLegislativeAssembly mla = mlaOptional.get();
                    issue.setMlaId(mla.getId());
                    issue.setMlaName(mla.getMemberName());
                    issue.setMlaParty(mla.getPartyName());
                    log.debug("Found MLA for issue: {} ({})", mla.getMemberName(), mla.getPartyName());
                } else if (ac.getCurrentMlaName() != null) {
                    // Use MLA info from AC record if direct link not found
                    issue.setMlaName(ac.getCurrentMlaName());
                    issue.setMlaParty(ac.getCurrentMlaParty());
                    log.debug("Using MLA from AC record: {}", ac.getCurrentMlaName());
                }
            }

            // Find Parliamentary Constituency and MP
            Optional<ParliamentaryConstituency> pcOptional = pcRepository.findByPoint(lat, lng);
            if (pcOptional.isPresent()) {
                ParliamentaryConstituency pc = pcOptional.get();
                issue.setParliamentaryConstituency(pc.getPcName());

                // Find MP for this constituency
                Optional<MemberOfParliament> mpOptional = mpRepository.findByConstituencyIdActive(pc.getId());
                if (mpOptional.isEmpty()) {
                    // Try by PC name and state
                    mpOptional = mpRepository.findByPcNameAndStateName(pc.getPcName(), pc.getStateName());
                }
                if (mpOptional.isPresent()) {
                    MemberOfParliament mp = mpOptional.get();
                    issue.setMpId(mp.getId());
                    issue.setMpName(mp.getMemberName());
                    issue.setMpParty(mp.getPartyName());
                    log.debug("Found MP for issue: {} ({})", mp.getMemberName(), mp.getPartyName());
                } else if (pc.getCurrentMpName() != null) {
                    // Use MP info from PC record if direct link not found
                    issue.setMpName(pc.getCurrentMpName());
                    issue.setMpParty(pc.getCurrentMpParty());
                    log.debug("Using MP from PC record: {}", pc.getCurrentMpName());
                }
            }
        } catch (Exception e) {
            log.warn("Error populating MLA/MP for issue at location ({}, {}): {}", lat, lng, e.getMessage());
            // Continue without MLA/MP info - non-critical error
        }
    }

    /**
     * Vote on an issue (upvote or downvote)
     */
    @Transactional
    public IssueDTO vote(Long issueId, Long userId, String voteType) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found: " + issueId));

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        IssueVote.VoteType newVoteType = IssueVote.VoteType.valueOf(voteType.toUpperCase());

        // Check for existing vote
        var existingVote = issueVoteRepository.findByIssueIdAndUserId(issueId, userId);

        if (existingVote.isPresent()) {
            IssueVote vote = existingVote.get();
            if (vote.getVoteType() == newVoteType) {
                // Same vote - remove it (toggle off)
                if (vote.getVoteType() == IssueVote.VoteType.UPVOTE) {
                    issue.setUpvoteCount(issue.getUpvoteCount() - 1);
                } else {
                    issue.setDownvoteCount(issue.getDownvoteCount() - 1);
                }
                issueVoteRepository.delete(vote);
                issueRepository.save(issue);
                return mapToDTO(issue, userId);
            } else {
                // Different vote - change it
                if (vote.getVoteType() == IssueVote.VoteType.UPVOTE) {
                    issue.setUpvoteCount(issue.getUpvoteCount() - 1);
                    issue.setDownvoteCount(issue.getDownvoteCount() + 1);
                } else {
                    issue.setDownvoteCount(issue.getDownvoteCount() - 1);
                    issue.setUpvoteCount(issue.getUpvoteCount() + 1);
                }
                vote.setVoteType(newVoteType);
                vote.setUpdatedAt(LocalDateTime.now());
                issueVoteRepository.save(vote);
                issueRepository.save(issue);
                return mapToDTO(issue, userId);
            }
        } else {
            // New vote
            IssueVote vote = new IssueVote(issue, user, newVoteType);
            issueVoteRepository.save(vote);

            if (newVoteType == IssueVote.VoteType.UPVOTE) {
                issue.setUpvoteCount(issue.getUpvoteCount() + 1);
            } else {
                issue.setDownvoteCount(issue.getDownvoteCount() + 1);
            }
            issueRepository.save(issue);
            return mapToDTO(issue, userId);
        }
    }

    /**
     * Remove vote from an issue
     */
    @Transactional
    public IssueDTO removeVote(Long issueId, Long userId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found: " + issueId));

        var existingVote = issueVoteRepository.findByIssueIdAndUserId(issueId, userId);
        if (existingVote.isPresent()) {
            IssueVote vote = existingVote.get();
            if (vote.getVoteType() == IssueVote.VoteType.UPVOTE) {
                issue.setUpvoteCount(issue.getUpvoteCount() - 1);
            } else {
                issue.setDownvoteCount(issue.getDownvoteCount() - 1);
            }
            issueVoteRepository.delete(vote);
            issueRepository.save(issue);
        }

        return mapToDTO(issue, userId);
    }

    /**
     * Update issue status (admin only)
     */
    @Transactional
    public IssueDTO updateStatus(Long issueId, String status) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found: " + issueId));

        Issue.IssueStatus newStatus = Issue.IssueStatus.valueOf(status.toUpperCase());
        issue.setStatus(newStatus);

        if (newStatus == Issue.IssueStatus.RESOLVED) {
            issue.setResolvedAt(LocalDateTime.now());
        }

        Issue saved = issueRepository.save(issue);
        return mapToDTO(saved, null);
    }

    /**
     * Assign issue to official
     */
    @Transactional
    public IssueDTO assignToOfficial(Long issueId, Long officialId, String officialName) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found: " + issueId));

        issue.setAssignedOfficialId(officialId);
        issue.setAssignedOfficialName(officialName);

        if (issue.getStatus() == Issue.IssueStatus.PENDING) {
            issue.setStatus(Issue.IssueStatus.ACKNOWLEDGED);
        }

        Issue saved = issueRepository.save(issue);
        return mapToDTO(saved, null);
    }

    /**
     * Get user stats for profile
     */
    @Transactional(readOnly = true)
    public UserIssueStats getUserStats(Long userId) {
        Long totalReported = issueRepository.countByReporterId(userId);
        Long totalResolved = issueRepository.countResolvedByReporterId(userId);
        Long totalVotes = issueVoteRepository.countByUserId(userId);

        return new UserIssueStats(totalReported, totalResolved, totalVotes);
    }

    // Helper methods

    private IssueDTO mapToDTO(Issue issue, Long userId) {
        String userVote = null;
        if (userId != null) {
            var vote = issueVoteRepository.findByIssueIdAndUserId(issue.getId(), userId);
            if (vote.isPresent()) {
                userVote = vote.get().getVoteType().name();
            }
        }
        return IssueDTO.fromEntity(issue, userVote);
    }

    private String generateTrackingId() {
        // Format: CIV-XXXX-XXXX
        String uuid = UUID.randomUUID().toString().toUpperCase().replace("-", "");
        return "CIV-" + uuid.substring(0, 4) + "-" + uuid.substring(4, 8);
    }

    /**
     * Backfill MLA and MP data for existing issues that have location but missing representative info
     */
    @Transactional
    public Map<String, Object> backfillMlaAndMpData() {
        log.info("Starting backfill of MLA/MP data for existing issues");

        List<Issue> issues = issueRepository.findAll();
        int updated = 0;
        int skipped = 0;
        int noLocation = 0;

        for (Issue issue : issues) {
            if (issue.getLatitude() == null || issue.getLongitude() == null) {
                noLocation++;
                continue;
            }

            // Skip if already has MLA/MP data
            if (issue.getMlaName() != null && issue.getMpName() != null) {
                skipped++;
                continue;
            }

            populateMlaAndMpFromLocation(issue);

            if (issue.getMlaName() != null || issue.getMpName() != null) {
                issueRepository.save(issue);
                updated++;
            }
        }

        log.info("Backfill completed: {} updated, {} skipped, {} no location", updated, skipped, noLocation);

        return Map.of(
            "message", "Backfill completed",
            "totalIssues", issues.size(),
            "updated", updated,
            "skipped", skipped,
            "noLocation", noLocation
        );
    }

    // Inner class for user stats
    public record UserIssueStats(Long issuesReported, Long issuesResolved, Long votesGiven) {}
}
