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
import com.civicconnect.api.dto.WardCouncillorDTO;
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
    private final WardCouncillorService wardCouncillorService;
    private final GeocodingService geocodingService;

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
        issue.setAudioUrl(request.getAudioUrl());
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

        // Auto-populate MLA, MP, and Councillor based on location
        if (issue.getLatitude() != null && issue.getLongitude() != null) {
            populateRepresentativesFromLocation(issue);
        }

        // Generate tracking ID
        issue.setTrackingId(generateTrackingId());

        Issue saved = issueRepository.save(issue);
        log.info("Created issue {} with tracking ID {} - MLA: {}, MP: {}",
                saved.getId(), saved.getTrackingId(), saved.getMlaName(), saved.getMpName());

        return mapToDTO(saved, userId);
    }

    /**
     * Auto-populate MLA, MP, and Councillor info based on issue location
     */
    private void populateRepresentativesFromLocation(Issue issue) {
        Double lat = issue.getLatitude();
        Double lng = issue.getLongitude();
        String cityName = null;
        String stateName = null;

        try {
            // Find Assembly Constituency and MLA
            Optional<AssemblyConstituency> acOptional = acRepository.findByPoint(lat, lng);
            if (acOptional.isPresent()) {
                AssemblyConstituency ac = acOptional.get();
                issue.setAssemblyConstituency(ac.getAcName());
                stateName = ac.getStateName();

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
                    log.info("Found MLA for issue: {} ({})", mla.getMemberName(), mla.getPartyName());
                } else if (ac.getCurrentMlaName() != null) {
                    // Use MLA info from AC record if direct link not found
                    issue.setMlaName(ac.getCurrentMlaName());
                    issue.setMlaParty(ac.getCurrentMlaParty());
                    log.info("Using MLA from AC record: {}", ac.getCurrentMlaName());
                }
            }

            // Find Parliamentary Constituency and MP
            Optional<ParliamentaryConstituency> pcOptional = pcRepository.findByPoint(lat, lng);
            if (pcOptional.isPresent()) {
                ParliamentaryConstituency pc = pcOptional.get();
                issue.setParliamentaryConstituency(pc.getPcName());
                if (stateName == null) {
                    stateName = pc.getStateName();
                }
                log.info("Found PC for issue: {} (ID: {}, State: {})", pc.getPcName(), pc.getId(), pc.getStateName());

                // Find MP for this constituency
                Optional<MemberOfParliament> mpOptional = mpRepository.findByConstituencyIdActive(pc.getId());
                log.debug("MP lookup by constituency ID {}: found={}", pc.getId(), mpOptional.isPresent());

                if (mpOptional.isEmpty()) {
                    // Try by PC name and state
                    mpOptional = mpRepository.findByPcNameAndStateName(pc.getPcName(), pc.getStateName());
                    log.debug("MP lookup by name '{}' and state '{}': found={}", pc.getPcName(), pc.getStateName(), mpOptional.isPresent());
                }

                if (mpOptional.isEmpty()) {
                    // Try partial match on constituency name
                    var mpList = mpRepository.findByConstituencyNameContainingIgnoreCaseAndIsActiveTrue(pc.getPcName());
                    if (!mpList.isEmpty()) {
                        mpOptional = Optional.of(mpList.get(0));
                        log.debug("MP lookup by partial name match '{}': found {} matches", pc.getPcName(), mpList.size());
                    }
                }

                if (mpOptional.isPresent()) {
                    MemberOfParliament mp = mpOptional.get();
                    issue.setMpId(mp.getId());
                    issue.setMpName(mp.getMemberName());
                    issue.setMpParty(mp.getPartyName());
                    log.info("Found MP for issue: {} ({})", mp.getMemberName(), mp.getPartyName());
                } else if (pc.getCurrentMpName() != null) {
                    // Use MP info from PC record if direct link not found
                    issue.setMpName(pc.getCurrentMpName());
                    issue.setMpParty(pc.getCurrentMpParty());
                    log.info("Using MP from PC record: {}", pc.getCurrentMpName());
                } else {
                    log.warn("No MP found for PC: {} (ID: {}). PC currentMpName is also null.", pc.getPcName(), pc.getId());
                }
            } else {
                log.warn("No Parliamentary Constituency found for location ({}, {})", lat, lng);
            }

            // Find Ward Councillor using reverse geocoding
            // For Delhi, city and state are the same
            cityName = issue.getDistrictName() != null ? issue.getDistrictName() : stateName;
            if (cityName != null) {
                populateCouncillorFromLocation(issue, lat, lng, cityName);
            }

        } catch (Exception e) {
            log.warn("Error populating representatives for issue at location ({}, {}): {}", lat, lng, e.getMessage());
            // Continue without representative info - non-critical error
        }
    }

    /**
     * Populate ward councillor info using reverse geocoding and locality matching
     */
    private void populateCouncillorFromLocation(Issue issue, Double lat, Double lng, String city) {
        try {
            // Check if councillor data exists for this city
            long councillorCount = wardCouncillorService.getCountByCity(city);
            if (councillorCount == 0) {
                // Try with state name for Delhi-like cases where city = state
                if (issue.getStateName() != null) {
                    councillorCount = wardCouncillorService.getCountByCity(issue.getStateName());
                    if (councillorCount > 0) {
                        city = issue.getStateName();
                    }
                }
            }

            if (councillorCount == 0) {
                log.debug("No councillor data available for city: {}", city);
                return;
            }

            log.info("Found {} councillors for city: {}, attempting geocoding lookup...", councillorCount, city);

            // Use Google Geocoding to get locality name
            Optional<GeocodingService.GeocodingResult> geocodeResult = geocodingService.reverseGeocode(lat, lng);

            if (geocodeResult.isEmpty()) {
                log.warn("Geocoding failed for ({}, {}), cannot match councillor", lat, lng);
                return;
            }

            GeocodingService.GeocodingResult geo = geocodeResult.get();
            log.info("Geocoding result: locality={}, sublocality={}, city={}",
                    geo.locality(), geo.sublocality(), geo.city());

            Optional<WardCouncillorDTO> councillor = Optional.empty();

            // Try locality first (neighborhood)
            if (geo.locality() != null) {
                councillor = wardCouncillorService.findByLocality(geo.locality(), city);
            }

            // Try sublocality if locality didn't match
            if (councillor.isEmpty() && geo.sublocalityLevel1() != null) {
                councillor = wardCouncillorService.findByLocality(geo.sublocalityLevel1(), city);
            }

            // Try sublocalityLevel2
            if (councillor.isEmpty() && geo.sublocalityLevel2() != null) {
                councillor = wardCouncillorService.findByLocality(geo.sublocalityLevel2(), city);
            }

            // Try address match
            if (councillor.isEmpty() && geo.formattedAddress() != null) {
                councillor = wardCouncillorService.findByAddress(geo.formattedAddress(), city);
            }

            // Try matching with Assembly Constituency name as last resort
            // This is useful because AC names often correspond to ward names in Delhi
            if (councillor.isEmpty() && issue.getAssemblyConstituency() != null) {
                log.info("Trying to match councillor using AC name: {}", issue.getAssemblyConstituency());
                councillor = wardCouncillorService.findByLocality(issue.getAssemblyConstituency(), city);
            }

            // Populate issue with councillor data
            if (councillor.isPresent()) {
                WardCouncillorDTO c = councillor.get();
                issue.setCouncillorId(c.getId());
                issue.setCouncillorName(c.getCouncillorName());
                issue.setCouncillorParty(c.getPartyAffiliation());
                issue.setWardNo(c.getWardNo());
                issue.setWardName(c.getWardName());
                log.info("Matched councillor for issue: {} (Ward {} - {})",
                        c.getCouncillorName(), c.getWardNo(), c.getWardName());
            } else {
                log.info("No councillor match found for locality: {} in city: {}", geo.locality(), city);
            }

        } catch (Exception e) {
            log.warn("Error populating councillor for issue at location ({}, {}): {}", lat, lng, e.getMessage());
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
     * Backfill MLA, MP, and Councillor data for existing issues that have location but missing representative info
     */
    @Transactional
    public Map<String, Object> backfillRepresentativeData() {
        log.info("Starting backfill of MLA/MP/Councillor data for existing issues");

        List<Issue> issues = issueRepository.findAll();
        int updated = 0;
        int skipped = 0;
        int noLocation = 0;

        for (Issue issue : issues) {
            if (issue.getLatitude() == null || issue.getLongitude() == null) {
                noLocation++;
                continue;
            }

            // Skip if already has all representative data
            if (issue.getMlaName() != null && issue.getMpName() != null && issue.getCouncillorName() != null) {
                skipped++;
                continue;
            }

            populateRepresentativesFromLocation(issue);

            if (issue.getMlaName() != null || issue.getMpName() != null || issue.getCouncillorName() != null) {
                issueRepository.save(issue);
                updated++;
                log.info("Updated issue {}: MLA={}, MP={}, Councillor={}",
                        issue.getId(), issue.getMlaName(), issue.getMpName(), issue.getCouncillorName());
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

    /**
     * Delete an issue (only by the reporter/owner)
     * @param issueId the ID of the issue to delete
     * @param userId the ID of the user requesting deletion
     * @return true if deleted successfully
     * @throws RuntimeException if issue not found or user is not the owner
     */
    @Transactional
    public boolean deleteIssue(Long issueId, Long userId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found: " + issueId));

        // Verify that the user is the reporter (owner) of the issue
        if (issue.getReporter() == null || !issue.getReporter().getId().equals(userId)) {
            throw new RuntimeException("You can only delete your own issues");
        }

        // Delete all votes associated with this issue first
        issueVoteRepository.deleteByIssueId(issueId);

        // Delete the issue
        issueRepository.delete(issue);

        log.info("Issue {} deleted by user {}", issueId, userId);
        return true;
    }

    // Inner class for user stats
    public record UserIssueStats(Long issuesReported, Long issuesResolved, Long votesGiven) {}
}
