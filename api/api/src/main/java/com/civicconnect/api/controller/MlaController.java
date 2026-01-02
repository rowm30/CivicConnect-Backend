package com.civicconnect.api.controller;

import com.civicconnect.api.dto.MlaDTO;
import com.civicconnect.api.entity.AssemblyConstituency;
import com.civicconnect.api.entity.MemberOfLegislativeAssembly;
import com.civicconnect.api.repository.AssemblyConstituencyRepository;
import com.civicconnect.api.repository.MemberOfLegislativeAssemblyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for MLA data
 * Provides endpoints for querying MLA information
 */
@RestController
@RequestMapping("/api/mla")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MlaController {

    private final MemberOfLegislativeAssemblyRepository mlaRepository;
    private final AssemblyConstituencyRepository acRepository;

    /**
     * Get all MLAs with Refine-compatible pagination
     * Supports filtering by state, party, and search query
     */
    @GetMapping
    public ResponseEntity<List<MlaDTO>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String stateName,
            @RequestParam(required = false) String partyName,
            @RequestParam(defaultValue = "0") int _start,
            @RequestParam(defaultValue = "10") int _end,
            @RequestParam(defaultValue = "id") String _sort,
            @RequestParam(defaultValue = "ASC") String _order
    ) {
        log.info("Getting MLAs - q: {}, state: {}, party: {}", q, stateName, partyName);

        int page = _start / Math.max(1, _end - _start);
        int size = Math.max(1, _end - _start);

        Sort sort = Sort.by(_order.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, _sort);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MemberOfLegislativeAssembly> result;

        if (stateName != null && !stateName.isEmpty() && partyName != null && !partyName.isEmpty()) {
            // Filter by both state and party
            result = mlaRepository.findByStateNameAndPartyNameContaining(stateName, partyName, pageable);
        } else if (stateName != null && !stateName.isEmpty()) {
            // Filter by state only
            if (q != null && !q.isEmpty()) {
                result = mlaRepository.searchByNameInState(q, stateName, pageable);
            } else {
                result = mlaRepository.findByStateName(stateName, pageable);
            }
        } else if (partyName != null && !partyName.isEmpty()) {
            // Filter by party only
            result = mlaRepository.findByPartyNameContaining(partyName, pageable);
        } else if (q != null && !q.isEmpty()) {
            // Search by name across all
            result = mlaRepository.searchByNamePaged(q, pageable);
        } else {
            // No filters - get all
            result = mlaRepository.findAll(pageable);
        }

        List<MlaDTO> mlas = result.getContent().stream()
                .map(MlaDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .header("Access-Control-Expose-Headers", "X-Total-Count")
                .body(mlas);
    }

    /**
     * Get list of all states with MLAs
     */
    @GetMapping("/states")
    public ResponseEntity<List<String>> getStates() {
        log.info("Getting all states with MLAs");
        List<String> states = mlaRepository.findDistinctStateNames();
        return ResponseEntity.ok(states);
    }

    /**
     * Get list of all parties
     */
    @GetMapping("/parties")
    public ResponseEntity<List<String>> getParties() {
        log.info("Getting all parties");
        List<String> parties = mlaRepository.findDistinctPartyNames();
        return ResponseEntity.ok(parties);
    }

    /**
     * Get all MLAs for a state
     */
    @GetMapping("/state/{stateName}")
    public ResponseEntity<Page<MlaDTO>> getMlasByState(
            @PathVariable String stateName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        log.info("Getting MLAs for state: {}", stateName);
        Page<MlaDTO> mlas = mlaRepository.findByStateNameOrderByAcNo(stateName, PageRequest.of(page, size))
                .map(MlaDTO::fromEntity);
        return ResponseEntity.ok(mlas);
    }

    /**
     * Get MLA by constituency
     */
    @GetMapping("/constituency")
    public ResponseEntity<MlaDTO> getMlaByConstituency(
            @RequestParam String constituency,
            @RequestParam String state
    ) {
        log.info("Getting MLA for constituency: {} in {}", constituency, state);
        return mlaRepository.findByConstituencyNameAndStateName(constituency, state)
                .map(mla -> ResponseEntity.ok(MlaDTO.fromEntity(mla)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get MLA by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<MlaDTO> getMlaById(@PathVariable Long id) {
        log.info("Getting MLA by ID: {}", id);
        return mlaRepository.findById(id)
                .map(mla -> ResponseEntity.ok(MlaDTO.fromEntity(mla)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search MLAs by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<MlaDTO>> searchMlas(@RequestParam String query) {
        log.info("Searching MLAs: {}", query);
        List<MlaDTO> mlas = mlaRepository.searchByName(query).stream()
                .map(MlaDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(mlas);
    }

    /**
     * Get MLAs by party in a state
     */
    @GetMapping("/party/{partyName}/state/{stateName}")
    public ResponseEntity<List<MlaDTO>> getMlasByParty(
            @PathVariable String partyName,
            @PathVariable String stateName
    ) {
        log.info("Getting MLAs for party: {} in state: {}", partyName, stateName);
        List<MlaDTO> mlas = mlaRepository.findByPartyNameAndStateName(partyName, stateName).stream()
                .map(MlaDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(mlas);
    }

    /**
     * Get party-wise count for a state
     */
    @GetMapping("/state/{stateName}/party-count")
    public ResponseEntity<List<Map<String, Object>>> getPartyWiseCount(@PathVariable String stateName) {
        log.info("Getting party-wise count for: {}", stateName);
        List<Object[]> counts = mlaRepository.countByPartyInState(stateName);
        List<Map<String, Object>> result = counts.stream()
                .map(row -> Map.of(
                        "party", row[0] != null ? row[0] : "Unknown",
                        "count", row[1]
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Get MLAs with criminal cases in a state
     */
    @GetMapping("/state/{stateName}/criminal-cases")
    public ResponseEntity<List<MlaDTO>> getMlasWithCriminalCases(@PathVariable String stateName) {
        log.info("Getting MLAs with criminal cases in: {}", stateName);
        List<MlaDTO> mlas = mlaRepository.findMlasWithCriminalCases(stateName).stream()
                .map(MlaDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(mlas);
    }

    /**
     * Get state-wise MLA count
     */
    @GetMapping("/stats/by-state")
    public ResponseEntity<List<Map<String, Object>>> getStateWiseCount() {
        log.info("Getting state-wise MLA count");
        List<Object[]> counts = mlaRepository.countByState();
        List<Map<String, Object>> result = counts.stream()
                .map(row -> Map.of(
                        "state", row[0] != null ? row[0] : "Unknown",
                        "count", row[1]
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Get count of MLAs in a state
     */
    @GetMapping("/state/{stateName}/count")
    public ResponseEntity<Long> getMlaCount(@PathVariable String stateName) {
        log.info("Getting MLA count for: {}", stateName);
        return ResponseEntity.ok(mlaRepository.countByStateName(stateName));
    }

    // ==================== SYNC & LOCATION ENDPOINTS ====================

    /**
     * Sync all MLAs with Assembly Constituencies
     * This updates the MLA records with AC references and updates AC records with MLA info
     */
    @PostMapping("/sync-with-ac")
    public ResponseEntity<Map<String, Object>> syncMlasWithAssemblyConstituencies(
            @RequestParam(required = false) String stateName
    ) {
        log.info("Syncing MLAs with Assembly Constituencies for state: {}", stateName != null ? stateName : "ALL");

        List<MemberOfLegislativeAssembly> mlas;
        if (stateName != null && !stateName.isEmpty()) {
            mlas = mlaRepository.findByStateName(stateName);
        } else {
            mlas = mlaRepository.findAll();
        }

        int linkedCount = 0;
        int unmatchedCount = 0;
        List<String> unmatched = new ArrayList<>();

        for (MemberOfLegislativeAssembly mla : mlas) {
            AssemblyConstituency ac = findMatchingConstituency(mla);
            if (ac != null) {
                // Link MLA to AC
                mla.setAssemblyConstituency(ac);
                if (mla.getDistrictName() == null) {
                    mla.setDistrictName(ac.getDistrictName());
                }

                // Update AC with MLA info
                ac.setCurrentMlaName(mla.getMemberName());
                ac.setCurrentMlaParty(mla.getPartyName());
                acRepository.save(ac);

                linkedCount++;
            } else {
                unmatchedCount++;
                unmatched.add(mla.getConstituencyName() + " (AC#" + mla.getAcNo() + ", " + mla.getStateName() + ")");
            }
        }

        mlaRepository.saveAll(mlas);

        return ResponseEntity.ok(Map.of(
            "message", "MLA-AC sync completed",
            "totalMlas", mlas.size(),
            "linked", linkedCount,
            "unmatched", unmatchedCount,
            "unmatchedList", unmatched.size() > 20 ? unmatched.subList(0, 20) : unmatched
        ));
    }

    private AssemblyConstituency findMatchingConstituency(MemberOfLegislativeAssembly mla) {
        // Strategy 1: Match by AC number and state
        if (mla.getAcNo() != null && mla.getAcNo() > 0) {
            var byAcNo = acRepository.findByAcNoAndStateName(mla.getAcNo(), mla.getStateName());
            if (!byAcNo.isEmpty()) return byAcNo.get(0);
        }

        // Strategy 2: Exact match by name
        if (mla.getConstituencyName() != null) {
            var byName = acRepository.findByAcNameAndStateName(mla.getConstituencyName(), mla.getStateName());
            if (!byName.isEmpty()) return byName.get(0);

            // Strategy 3: Fuzzy match
            var fuzzy = acRepository.findByAcNameFuzzyAndStateName(mla.getConstituencyName(), mla.getStateName());
            if (!fuzzy.isEmpty()) return fuzzy.get(0);
        }

        return null;
    }

    /**
     * Get MLA by location (lat/long)
     * This is the key feature - given user's location, return their MLA
     */
    @GetMapping("/by-location")
    public ResponseEntity<?> getMlaByLocation(
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        log.info("Finding MLA for location: {}, {}", lat, lng);

        // First find the Assembly Constituency containing this point
        var acOptional = acRepository.findByPoint(lat, lng);

        if (acOptional.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "found", false,
                "message", "No Assembly Constituency found for this location"
            ));
        }

        AssemblyConstituency ac = acOptional.get();

        // Find the MLA for this constituency
        var mlaOptional = mlaRepository.findByAssemblyConstituencyId(ac.getId());

        if (mlaOptional.isEmpty()) {
            // Try finding by AC number and state
            mlaOptional = mlaRepository.findByAcNoAndStateName(ac.getAcNo(), ac.getStateName());
        }

        if (mlaOptional.isPresent()) {
            MemberOfLegislativeAssembly mla = mlaOptional.get();
            return ResponseEntity.ok(Map.of(
                "found", true,
                "constituency", Map.of(
                    "id", ac.getId(),
                    "acNo", ac.getAcNo(),
                    "acName", ac.getAcName(),
                    "stateName", ac.getStateName(),
                    "districtName", ac.getDistrictName() != null ? ac.getDistrictName() : "",
                    "reservedCategory", ac.getReservedCategory() != null ? ac.getReservedCategory() : "GEN"
                ),
                "mla", MlaDTO.fromEntity(mla)
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                "found", true,
                "constituency", Map.of(
                    "id", ac.getId(),
                    "acNo", ac.getAcNo(),
                    "acName", ac.getAcName(),
                    "stateName", ac.getStateName(),
                    "districtName", ac.getDistrictName() != null ? ac.getDistrictName() : "",
                    "reservedCategory", ac.getReservedCategory() != null ? ac.getReservedCategory() : "GEN",
                    "currentMlaName", ac.getCurrentMlaName() != null ? ac.getCurrentMlaName() : "",
                    "currentMlaParty", ac.getCurrentMlaParty() != null ? ac.getCurrentMlaParty() : ""
                ),
                "mla", (Object) null,
                "message", "Constituency found but no MLA record linked. MLA info from AC: " + ac.getCurrentMlaName()
            ));
        }
    }
}
