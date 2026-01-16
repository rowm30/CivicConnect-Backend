package com.civicconnect.api.service;

import com.civicconnect.api.dto.GovMapDTO.*;
import com.civicconnect.api.dto.RepresentativeRatingDTO;
import com.civicconnect.api.entity.*;
import com.civicconnect.api.entity.RepresentativeRating.RepresentativeType;
import com.civicconnect.api.entity.analytics.AppUser;
import com.civicconnect.api.repository.*;
import com.civicconnect.api.repository.analytics.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for building fixed government hierarchy structure.
 *
 * India's Democratic Hierarchy (Fixed Structure):
 *
 * NATIONAL: Citizen → MP → Prime Minister
 * STATE: Citizen → MLA → Chief Minister
 * LOCAL: Citizen → Ward Councillor → Mayor
 *
 * Each hierarchy ALWAYS shows all nodes, regardless of data availability.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GovMapHierarchyService {

    private final PrimeMinisterRepository primeMinisterRepository;
    private final ChiefMinisterRepository chiefMinisterRepository;
    private final MayorRepository mayorRepository;
    private final MemberOfLegislativeAssemblyRepository mlaRepository;
    private final MemberOfParliamentRepository mpRepository;
    private final WardCouncillorRepository wardCouncillorRepository;
    private final RepresentativeRatingService ratingService;
    private final AppUserRepository appUserRepository;

    /**
     * Get complete Gov Map with all three hierarchies
     */
    public CompleteGovMapResponse getCompleteGovMap(
            Long userId,
            Double latitude,
            Double longitude,
            String stateCode,
            String stateName,
            String cityName,
            Long assemblyConstituencyId,
            Long parliamentaryConstituencyId
    ) {
        log.info("Building complete GovMap for user: {}, state: {}, city: {}", userId, stateName, cityName);

        // Build citizen info
        CitizenInfo citizenInfo = buildCitizenInfo(userId);

        // Build all three hierarchies
        FixedHierarchyResponse localHierarchy = buildLocalHierarchy(
                userId, stateName, cityName, latitude, longitude);

        FixedHierarchyResponse stateHierarchy = buildStateHierarchy(
                userId, stateCode, stateName, assemblyConstituencyId);

        FixedHierarchyResponse nationalHierarchy = buildNationalHierarchy(
                userId, stateCode, stateName, parliamentaryConstituencyId);

        // Build location string
        String formattedLocation = buildFormattedLocation(cityName, stateName);
        boolean isLocationAvailable = stateName != null && !stateName.isEmpty();

        return CompleteGovMapResponse.builder()
                .citizen(citizenInfo)
                .localHierarchy(localHierarchy)
                .stateHierarchy(stateHierarchy)
                .nationalHierarchy(nationalHierarchy)
                .formattedLocation(formattedLocation)
                .isLocationAvailable(isLocationAvailable)
                .locationMessage(isLocationAvailable ? null : "Enable location to see your representatives")
                .build();
    }

    /**
     * Build LOCAL hierarchy: Citizen → Ward Councillor → Mayor
     */
    public FixedHierarchyResponse buildLocalHierarchy(
            Long userId,
            String stateName,
            String cityName,
            Double latitude,
            Double longitude
    ) {
        List<HierarchyNode> nodes = new ArrayList<>();

        // 1. Citizen node (always present)
        nodes.add(buildCitizenNode(userId));

        // 2. Ward Councillor node
        HierarchyNode councillorNode = buildWardCouncillorNode(userId, stateName, cityName, latitude, longitude);
        nodes.add(councillorNode);

        // 3. Mayor node
        HierarchyNode mayorNode = buildMayorNode(userId, stateName, cityName);
        nodes.add(mayorNode);

        // Build summary
        HierarchySummary summary = buildSummary(nodes);

        return FixedHierarchyResponse.builder()
                .mode("LOCAL")
                .modeName("Local Government")
                .modeDescription("Your municipal representatives who handle local civic issues like water, roads, sanitation")
                .stateName(stateName)
                .cityName(cityName)
                .nodes(nodes)
                .citizen(buildCitizenInfo(userId))
                .summary(summary)
                .build();
    }

    /**
     * Build STATE hierarchy: Citizen → MLA → Chief Minister
     */
    public FixedHierarchyResponse buildStateHierarchy(
            Long userId,
            String stateCode,
            String stateName,
            Long assemblyConstituencyId
    ) {
        List<HierarchyNode> nodes = new ArrayList<>();

        // 1. Citizen node
        nodes.add(buildCitizenNode(userId));

        // 2. MLA node
        HierarchyNode mlaNode = buildMLANode(userId, stateCode, stateName, assemblyConstituencyId);
        nodes.add(mlaNode);

        // 3. Chief Minister node
        HierarchyNode cmNode = buildChiefMinisterNode(userId, stateCode, stateName);
        nodes.add(cmNode);

        // Build summary
        HierarchySummary summary = buildSummary(nodes);

        return FixedHierarchyResponse.builder()
                .mode("STATE")
                .modeName("State Government")
                .modeDescription("Your state representatives who handle education, health, law & order")
                .stateName(stateName)
                .stateCode(stateCode)
                .nodes(nodes)
                .citizen(buildCitizenInfo(userId))
                .summary(summary)
                .build();
    }

    /**
     * Build NATIONAL hierarchy: Citizen → MP → Prime Minister
     */
    public FixedHierarchyResponse buildNationalHierarchy(
            Long userId,
            String stateCode,
            String stateName,
            Long parliamentaryConstituencyId
    ) {
        List<HierarchyNode> nodes = new ArrayList<>();

        // 1. Citizen node
        nodes.add(buildCitizenNode(userId));

        // 2. MP node
        HierarchyNode mpNode = buildMPNode(userId, stateCode, stateName, parliamentaryConstituencyId);
        nodes.add(mpNode);

        // 3. Prime Minister node
        HierarchyNode pmNode = buildPrimeMinisterNode(userId);
        nodes.add(pmNode);

        // Build summary
        HierarchySummary summary = buildSummary(nodes);

        return FixedHierarchyResponse.builder()
                .mode("NATIONAL")
                .modeName("National Government")
                .modeDescription("Your national representatives in Parliament and the Central Government")
                .stateName(stateName)
                .stateCode(stateCode)
                .nodes(nodes)
                .citizen(buildCitizenInfo(userId))
                .summary(summary)
                .build();
    }

    // ========================
    // Node Building Methods
    // ========================

    private HierarchyNode buildCitizenNode(Long userId) {
        AppUser user = userId != null ? appUserRepository.findById(userId).orElse(null) : null;
        String userName = user != null ? user.getName() : "Citizen";

        return HierarchyNode.builder()
                .nodeType("CITIZEN")
                .designation("Citizen")
                .designationHindi("नागरिक")
                .level(0)
                .isDataAvailable(true)
                .id(userId)
                .name(userName)
                .canRate(false)
                .build();
    }

    private HierarchyNode buildWardCouncillorNode(
            Long userId,
            String stateName,
            String cityName,
            Double latitude,
            Double longitude
    ) {
        HierarchyNode.HierarchyNodeBuilder builder = HierarchyNode.builder()
                .nodeType("WARD_COUNCILLOR")
                .designation("Ward Councillor")
                .designationHindi("पार्षद")
                .level(1);

        try {
            // Try to find councillor by location or city
            Optional<WardCouncillor> councillorOpt = Optional.empty();

            if (latitude != null && longitude != null) {
                councillorOpt = wardCouncillorRepository.findByLocation(latitude, longitude);
            }

            if (councillorOpt.isEmpty() && cityName != null) {
                // Get any councillor from the city as a fallback
                List<WardCouncillor> councillors = wardCouncillorRepository.findByCityNameIgnoreCase(cityName);
                if (!councillors.isEmpty()) {
                    councillorOpt = Optional.of(councillors.get(0));
                }
            }

            if (councillorOpt.isPresent()) {
                WardCouncillor councillor = councillorOpt.get();

                // Get rating stats
                RepresentativeRatingDTO.RepresentativeStats stats = null;
                try {
                    stats = ratingService.getRepresentativeStats(
                            RepresentativeType.WARD_COUNCILLOR,
                            councillor.getId(),
                            councillor.getCouncillorName(),
                            councillor.getPartyAffiliation(),
                            councillor.getWardName()
                    );
                } catch (Exception e) {
                    log.warn("Could not get rating stats for councillor: {}", e.getMessage());
                }

                Integer userRating = getUserRating(userId, RepresentativeType.WARD_COUNCILLOR, councillor.getId());

                return builder
                        .isDataAvailable(true)
                        .id(councillor.getId())
                        .name(councillor.getCouncillorName())
                        .party(councillor.getPartyAffiliation())
                        .photoUrl(councillor.getPhotoUrl())
                        .constituency(councillor.getWardName())
                        .city(councillor.getCity())
                        .state(councillor.getState())
                        .approvalRating(stats != null ? stats.getApprovalRating() : null)
                        .totalRatings(stats != null ? stats.getTotalRatings() : 0L)
                        .approvalLevel(stats != null ? stats.getApprovalLevel() : "Not Rated")
                        .accountabilityScore(stats != null ? stats.getAccountabilityScore() : null)
                        .issuesRegistered(stats != null ? stats.getIssuesRegistered() : 0L)
                        .issuesResolved(stats != null ? stats.getIssuesResolved() : 0L)
                        .resolutionRate(stats != null ? stats.getResolutionRate() : 0.0)
                        .email(councillor.getEmail())
                        .phone(councillor.getPhone())
                        .userRating(userRating)
                        .canRate(true)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error finding ward councillor: {}", e.getMessage());
        }

        // Data not available
        return builder
                .isDataAvailable(false)
                .canRate(false)
                .unavailableMessage("Ward councillor data not available for this area. We're working on adding this information.")
                .build();
    }

    private HierarchyNode buildMayorNode(Long userId, String stateName, String cityName) {
        HierarchyNode.HierarchyNodeBuilder builder = HierarchyNode.builder()
                .nodeType("MAYOR")
                .designation("Mayor")
                .designationHindi("महापौर")
                .level(2);

        try {
            if (cityName != null && !cityName.isEmpty()) {
                Optional<Mayor> mayorOpt = stateName != null ?
                        mayorRepository.findByCityAndStateAndStatus(cityName, stateName, Mayor.MayorStatus.CURRENT) :
                        mayorRepository.findByCityNameIgnoreCaseAndStatus(cityName, Mayor.MayorStatus.CURRENT);

                if (mayorOpt.isPresent()) {
                    Mayor mayor = mayorOpt.get();

                    RepresentativeRatingDTO.RepresentativeStats stats = null;
                    try {
                        stats = ratingService.getRepresentativeStats(
                                RepresentativeType.MAYOR,
                                mayor.getId(),
                                mayor.getName(),
                                mayor.getPartyAbbreviation(),
                                mayor.getCityName()
                        );
                    } catch (Exception e) {
                        log.warn("Could not get rating stats for mayor: {}", e.getMessage());
                    }

                    Integer userRating = getUserRating(userId, RepresentativeType.MAYOR, mayor.getId());

                    return builder
                            .isDataAvailable(true)
                            .id(mayor.getId())
                            .name(mayor.getName())
                            .party(mayor.getPartyName())
                            .partyAbbreviation(mayor.getPartyAbbreviation())
                            .photoUrl(mayor.getPhotoUrl())
                            .city(mayor.getCityName())
                            .state(mayor.getStateName())
                            .approvalRating(stats != null ? stats.getApprovalRating() : null)
                            .totalRatings(stats != null ? stats.getTotalRatings() : 0L)
                            .approvalLevel(stats != null ? stats.getApprovalLevel() : "Not Rated")
                            .accountabilityScore(stats != null ? stats.getAccountabilityScore() : null)
                            .issuesRegistered(mayor.getTotalIssuesInCity())
                            .issuesResolved(mayor.getResolvedIssuesInCity())
                            .resolutionRate(mayor.getResolutionRate())
                            .email(mayor.getEmail())
                            .phone(mayor.getPhone())
                            .twitterHandle(mayor.getTwitterHandle())
                            .userRating(userRating)
                            .canRate(true)
                            .build();
                }
            }
        } catch (Exception e) {
            log.error("Error finding mayor: {}", e.getMessage());
        }

        return builder
                .isDataAvailable(false)
                .canRate(false)
                .unavailableMessage("Mayor/Municipal Commissioner data not available for this city.")
                .build();
    }

    private HierarchyNode buildMLANode(Long userId, String stateCode, String stateName, Long acId) {
        HierarchyNode.HierarchyNodeBuilder builder = HierarchyNode.builder()
                .nodeType("MLA")
                .designation("Member of Legislative Assembly")
                .designationHindi("विधायक (MLA)")
                .level(1);

        try {
            Optional<MemberOfLegislativeAssembly> mlaOpt = Optional.empty();

            if (acId != null) {
                mlaOpt = mlaRepository.findCurrentMlaByAcId(acId);
            }

            if (mlaOpt.isPresent()) {
                MemberOfLegislativeAssembly mla = mlaOpt.get();

                RepresentativeRatingDTO.RepresentativeStats stats = null;
                try {
                    stats = ratingService.getRepresentativeStats(
                            RepresentativeType.MLA,
                            mla.getId(),
                            mla.getMemberName(),
                            mla.getPartyAbbreviation(),
                            mla.getConstituencyName()
                    );
                } catch (Exception e) {
                    log.warn("Could not get rating stats for MLA: {}", e.getMessage());
                }

                Integer userRating = getUserRating(userId, RepresentativeType.MLA, mla.getId());

                return builder
                        .isDataAvailable(true)
                        .id(mla.getId())
                        .name(mla.getMemberName())
                        .party(mla.getPartyName())
                        .partyAbbreviation(mla.getPartyAbbreviation())
                        .photoUrl(mla.getPhotoUrl())
                        .constituency(mla.getConstituencyName())
                        .state(mla.getStateName())
                        .approvalRating(stats != null ? stats.getApprovalRating() : null)
                        .totalRatings(stats != null ? stats.getTotalRatings() : 0L)
                        .approvalLevel(stats != null ? stats.getApprovalLevel() : "Not Rated")
                        .accountabilityScore(stats != null ? stats.getAccountabilityScore() : null)
                        .issuesRegistered(stats != null ? stats.getIssuesRegistered() : 0L)
                        .issuesResolved(stats != null ? stats.getIssuesResolved() : 0L)
                        .resolutionRate(stats != null ? stats.getResolutionRate() : 0.0)
                        .email(mla.getEmail())
                        .phone(mla.getPhone())
                        .userRating(userRating)
                        .canRate(true)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error finding MLA: {}", e.getMessage());
        }

        return builder
                .isDataAvailable(false)
                .canRate(false)
                .unavailableMessage("MLA data not available for your Assembly Constituency.")
                .build();
    }

    private HierarchyNode buildChiefMinisterNode(Long userId, String stateCode, String stateName) {
        HierarchyNode.HierarchyNodeBuilder builder = HierarchyNode.builder()
                .nodeType("CHIEF_MINISTER")
                .designation("Chief Minister")
                .designationHindi("मुख्यमंत्री")
                .level(2);

        try {
            Optional<ChiefMinister> cmOpt = Optional.empty();

            if (stateCode != null) {
                cmOpt = chiefMinisterRepository.findByStateCodeAndStatus(stateCode, ChiefMinister.CMStatus.CURRENT);
            } else if (stateName != null) {
                cmOpt = chiefMinisterRepository.findByStateNameIgnoreCaseAndStatus(stateName, ChiefMinister.CMStatus.CURRENT);
            }

            if (cmOpt.isPresent()) {
                ChiefMinister cm = cmOpt.get();

                RepresentativeRatingDTO.RepresentativeStats stats = null;
                try {
                    stats = ratingService.getRepresentativeStats(
                            RepresentativeType.CHIEF_MINISTER,
                            cm.getId(),
                            cm.getName(),
                            cm.getPartyAbbreviation(),
                            cm.getStateName()
                    );
                } catch (Exception e) {
                    log.warn("Could not get rating stats for CM: {}", e.getMessage());
                }

                Integer userRating = getUserRating(userId, RepresentativeType.CHIEF_MINISTER, cm.getId());

                return builder
                        .isDataAvailable(true)
                        .id(cm.getId())
                        .name(cm.getName())
                        .party(cm.getPartyName())
                        .partyAbbreviation(cm.getPartyAbbreviation())
                        .photoUrl(cm.getPhotoUrl())
                        .state(cm.getStateName())
                        .approvalRating(stats != null ? stats.getApprovalRating() : null)
                        .totalRatings(stats != null ? stats.getTotalRatings() : 0L)
                        .approvalLevel(stats != null ? stats.getApprovalLevel() : "Not Rated")
                        .accountabilityScore(stats != null ? stats.getAccountabilityScore() : null)
                        .issuesRegistered(cm.getTotalIssuesInState())
                        .issuesResolved(cm.getResolvedIssuesInState())
                        .resolutionRate(cm.getResolutionRate())
                        .email(cm.getEmail())
                        .phone(cm.getPhone())
                        .twitterHandle(cm.getTwitterHandle())
                        .userRating(userRating)
                        .canRate(true)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error finding Chief Minister: {}", e.getMessage());
        }

        return builder
                .isDataAvailable(false)
                .canRate(false)
                .unavailableMessage("Chief Minister data not available for this state.")
                .build();
    }

    private HierarchyNode buildMPNode(Long userId, String stateCode, String stateName, Long pcId) {
        HierarchyNode.HierarchyNodeBuilder builder = HierarchyNode.builder()
                .nodeType("MP")
                .designation("Member of Parliament")
                .designationHindi("सांसद (MP)")
                .level(1);

        try {
            Optional<MemberOfParliament> mpOpt = Optional.empty();

            if (pcId != null) {
                mpOpt = mpRepository.findCurrentMpByConstituencyId(pcId);
            }

            if (mpOpt.isPresent()) {
                MemberOfParliament mp = mpOpt.get();

                RepresentativeRatingDTO.RepresentativeStats stats = null;
                try {
                    stats = ratingService.getRepresentativeStats(
                            RepresentativeType.MP,
                            mp.getId(),
                            mp.getMemberName(),
                            mp.getPartyAbbreviation(),
                            mp.getConstituencyName()
                    );
                } catch (Exception e) {
                    log.warn("Could not get rating stats for MP: {}", e.getMessage());
                }

                Integer userRating = getUserRating(userId, RepresentativeType.MP, mp.getId());

                return builder
                        .isDataAvailable(true)
                        .id(mp.getId())
                        .name(mp.getMemberName())
                        .party(mp.getPartyName())
                        .partyAbbreviation(mp.getPartyAbbreviation())
                        .photoUrl(mp.getPhotoUrl())
                        .constituency(mp.getConstituencyName())
                        .state(mp.getStateName())
                        .approvalRating(stats != null ? stats.getApprovalRating() : null)
                        .totalRatings(stats != null ? stats.getTotalRatings() : 0L)
                        .approvalLevel(stats != null ? stats.getApprovalLevel() : "Not Rated")
                        .accountabilityScore(stats != null ? stats.getAccountabilityScore() : null)
                        .issuesRegistered(stats != null ? stats.getIssuesRegistered() : 0L)
                        .issuesResolved(stats != null ? stats.getIssuesResolved() : 0L)
                        .resolutionRate(stats != null ? stats.getResolutionRate() : 0.0)
                        .email(mp.getEmail())
                        .phone(mp.getPhone())
                        .userRating(userRating)
                        .canRate(true)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error finding MP: {}", e.getMessage());
        }

        return builder
                .isDataAvailable(false)
                .canRate(false)
                .unavailableMessage("MP data not available for your Parliamentary Constituency.")
                .build();
    }

    private HierarchyNode buildPrimeMinisterNode(Long userId) {
        HierarchyNode.HierarchyNodeBuilder builder = HierarchyNode.builder()
                .nodeType("PRIME_MINISTER")
                .designation("Prime Minister")
                .designationHindi("प्रधानमंत्री")
                .level(2);

        try {
            Optional<PrimeMinister> pmOpt = primeMinisterRepository.findCurrentPM();

            if (pmOpt.isPresent()) {
                PrimeMinister pm = pmOpt.get();

                RepresentativeRatingDTO.RepresentativeStats stats = null;
                try {
                    stats = ratingService.getRepresentativeStats(
                            RepresentativeType.PRIME_MINISTER,
                            pm.getId(),
                            pm.getName(),
                            pm.getPartyAbbreviation(),
                            "India"
                    );
                } catch (Exception e) {
                    log.warn("Could not get rating stats for PM: {}", e.getMessage());
                }

                Integer userRating = getUserRating(userId, RepresentativeType.PRIME_MINISTER, pm.getId());

                return builder
                        .isDataAvailable(true)
                        .id(pm.getId())
                        .name(pm.getName())
                        .party(pm.getPartyName())
                        .partyAbbreviation(pm.getPartyAbbreviation())
                        .photoUrl(pm.getPhotoUrl())
                        .constituency(pm.getConstituencyName())
                        .state("India")
                        .approvalRating(stats != null ? stats.getApprovalRating() : null)
                        .totalRatings(stats != null ? stats.getTotalRatings() : 0L)
                        .approvalLevel(stats != null ? stats.getApprovalLevel() : "Not Rated")
                        .accountabilityScore(stats != null ? stats.getAccountabilityScore() : null)
                        .issuesRegistered(pm.getTotalIssuesNationwide())
                        .issuesResolved(pm.getResolvedIssuesNationwide())
                        .resolutionRate(pm.getResolutionRate())
                        .email(pm.getEmail())
                        .phone(pm.getPhone())
                        .twitterHandle(pm.getTwitterHandle())
                        .userRating(userRating)
                        .canRate(true)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error finding Prime Minister: {}", e.getMessage());
        }

        // Return hardcoded data for PM as fallback since we always know the PM
        return builder
                .isDataAvailable(true)
                .id(1L)
                .name("Narendra Modi")
                .party("Bharatiya Janata Party")
                .partyAbbreviation("BJP")
                .constituency("Varanasi")
                .state("India")
                .twitterHandle("@naaboramodi")
                .canRate(true)
                .approvalLevel("Not Rated")
                .build();
    }

    // ========================
    // Helper Methods
    // ========================

    private CitizenInfo buildCitizenInfo(Long userId) {
        if (userId == null) {
            return CitizenInfo.builder()
                    .name("Citizen")
                    .voicePower(0)
                    .voicePowerMessage("Login to track your civic participation")
                    .build();
        }

        try {
            AppUser user = appUserRepository.findById(userId).orElse(null);
            if (user == null) {
                return CitizenInfo.builder()
                        .userId(userId)
                        .name("Citizen")
                        .voicePower(0)
                        .voicePowerMessage("Start reporting issues to build your voice power")
                        .build();
            }

            // Count user's issues and ratings
            long issuesReported = 0; // TODO: Count from Issue repository
            long ratingsGiven = 0;   // TODO: Count from Rating repository
            int voicePower = calculateVoicePower(issuesReported, ratingsGiven);

            return CitizenInfo.builder()
                    .userId(userId)
                    .name(user.getName())
                    .issuesReported(issuesReported)
                    .ratingsGiven(ratingsGiven)
                    .voicePower(voicePower)
                    .voicePowerMessage(getVoicePowerMessage(voicePower))
                    .build();
        } catch (Exception e) {
            log.error("Error building citizen info: {}", e.getMessage());
            return CitizenInfo.builder()
                    .userId(userId)
                    .name("Citizen")
                    .voicePower(0)
                    .build();
        }
    }

    private Integer getUserRating(Long userId, RepresentativeType type, Long representativeId) {
        if (userId == null || representativeId == null) return null;
        try {
            return ratingService.getUserRating(userId, type, representativeId)
                    .map(RepresentativeRatingDTO.RatingResponse::getRating)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private int calculateVoicePower(long issuesReported, long ratingsGiven) {
        // Simple formula: 10 points per issue, 5 points per rating, max 100
        long score = (issuesReported * 10) + (ratingsGiven * 5);
        return (int) Math.min(100, score);
    }

    private String getVoicePowerMessage(int voicePower) {
        if (voicePower >= 80) return "You're a civic champion!";
        if (voicePower >= 60) return "Your voice is being heard!";
        if (voicePower >= 40) return "Keep engaging with your representatives!";
        if (voicePower >= 20) return "You're making a difference!";
        return "Start your civic journey today!";
    }

    private String buildFormattedLocation(String cityName, String stateName) {
        if (cityName != null && stateName != null) {
            return cityName + ", " + stateName;
        } else if (stateName != null) {
            return stateName;
        } else if (cityName != null) {
            return cityName;
        }
        return "Location not available";
    }

    private HierarchySummary buildSummary(List<HierarchyNode> nodes) {
        int totalNodes = nodes.size();
        int nodesWithData = (int) nodes.stream().filter(n -> Boolean.TRUE.equals(n.getIsDataAvailable())).count();
        int nodesWithoutData = totalNodes - nodesWithData;

        long totalIssues = nodes.stream()
                .filter(n -> n.getIssuesRegistered() != null)
                .mapToLong(HierarchyNode::getIssuesRegistered)
                .sum();

        long resolvedIssues = nodes.stream()
                .filter(n -> n.getIssuesResolved() != null)
                .mapToLong(HierarchyNode::getIssuesResolved)
                .sum();

        double avgAccountability = nodes.stream()
                .filter(n -> n.getAccountabilityScore() != null)
                .mapToInt(HierarchyNode::getAccountabilityScore)
                .average()
                .orElse(0.0);

        return HierarchySummary.builder()
                .totalNodesInHierarchy(totalNodes)
                .nodesWithData(nodesWithData)
                .nodesWithoutData(nodesWithoutData)
                .totalIssuesInArea(totalIssues)
                .resolvedIssuesInArea(resolvedIssues)
                .overallResolutionRate(totalIssues > 0 ? (resolvedIssues * 100.0 / totalIssues) : 0.0)
                .averageAccountabilityScore(avgAccountability)
                .build();
    }
}
