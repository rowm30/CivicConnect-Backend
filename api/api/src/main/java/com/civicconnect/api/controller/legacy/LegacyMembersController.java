package com.civicconnect.api.controller.legacy;

import com.civicconnect.api.dto.AssemblyConstituencyDTO;
import com.civicconnect.api.dto.MemberOfParliamentDTO;
import com.civicconnect.api.service.AssemblyConstituencyService;
import com.civicconnect.api.service.MemberOfParliamentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Legacy API compatibility layer for Android app.
 * Provides sitting-members and MLAs endpoints.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LegacyMembersController {

    private final MemberOfParliamentService mpService;
    private final AssemblyConstituencyService acService;

    /**
     * Android calls: GET /sitting-members/by-constituency?constituency=NAME
     * Returns list of sitting MPs for a constituency
     */
    @GetMapping("/sitting-members/by-constituency")
    public ResponseEntity<List<Map<String, Object>>> getSittingMembersByConstituency(
            @RequestParam String constituency) {

        log.info("Legacy API: /sitting-members/by-constituency called with constituency={}", constituency);

        List<Map<String, Object>> members = new ArrayList<>();

        try {
            // Search MPs by constituency name
            List<MemberOfParliamentDTO> mps = mpService.findByConstituencyName(constituency);

            for (MemberOfParliamentDTO mp : mps) {
                Map<String, Object> member = new HashMap<>();
                member.put("id", mp.getId());
                member.put("nameOfMember", mp.getMemberName());
                member.put("lastName", extractLastName(mp.getMemberName()));
                member.put("partyName", mp.getPartyName());
                member.put("constituency", mp.getConstituencyName());
                member.put("state", mp.getStateName());
                member.put("membershipStatus", mp.getMembershipStatus() != null ? mp.getMembershipStatus() : "Sitting");
                member.put("lokSabhaTerms", mp.getLokSabhaTerms());
                member.put("designation", "MP");
                members.add(member);
            }

        } catch (Exception e) {
            log.error("Error getting sitting members by constituency", e);
        }

        return ResponseEntity.ok(members);
    }

    /**
     * Android calls: POST /mlas/by-constituency with body {constituencyName: "..."}
     * Returns MLA info for assembly constituency
     */
    @PostMapping("/mlas/by-constituency")
    public ResponseEntity<List<Map<String, Object>>> getMlasByConstituency(
            @RequestBody Map<String, String> request) {

        String constituencyName = request.get("constituencyName");
        log.info("Legacy API: /mlas/by-constituency called with constituencyName={}", constituencyName);

        List<Map<String, Object>> mlas = new ArrayList<>();

        try {
            // Search assembly constituencies by name
            List<AssemblyConstituencyDTO> acs = acService.searchByName(constituencyName);

            for (AssemblyConstituencyDTO ac : acs) {
                if (ac.getCurrentMlaName() != null) {
                    Map<String, Object> mla = new HashMap<>();
                    mla.put("stateCode", ac.getStateCode());
                    mla.put("constituencyName", ac.getAcName());
                    mla.put("mlaName", ac.getCurrentMlaName());
                    mla.put("party", ac.getCurrentMlaParty());
                    mla.put("constituencyNumber", ac.getAcId());
                    mla.put("seatType", ac.getReservedCategory());
                    mla.put("isVacant", "No");
                    mlas.add(mla);
                }
            }

        } catch (Exception e) {
            log.error("Error getting MLAs by constituency", e);
        }

        return ResponseEntity.ok(mlas);
    }

    /**
     * Android calls: GET /mlas/all-constituencies
     * Returns all assembly constituencies with basic info
     */
    @GetMapping("/mlas/all-constituencies")
    public ResponseEntity<List<Map<String, Object>>> getAllConstituencies() {

        log.info("Legacy API: /mlas/all-constituencies called");

        try {
            List<AssemblyConstituencyDTO> constituencies = acService.getAllActiveConstituencies();

            List<Map<String, Object>> result = constituencies.stream()
                    .map(ac -> {
                        Map<String, Object> info = new HashMap<>();
                        info.put("constituencyName", ac.getAcName());
                        info.put("stateCode", ac.getStateCode());
                        info.put("stateName", ac.getStateName());
                        info.put("vacant", ac.getCurrentMlaName() == null);
                        return info;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error getting all constituencies", e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    /**
     * Android calls: GET /mlas/search-constituencies?query=...
     * Search constituencies by name
     */
    @GetMapping("/mlas/search-constituencies")
    public ResponseEntity<List<Map<String, Object>>> searchConstituencies(
            @RequestParam String query) {

        log.info("Legacy API: /mlas/search-constituencies called with query={}", query);

        try {
            List<AssemblyConstituencyDTO> constituencies = acService.searchByName(query);

            List<Map<String, Object>> result = constituencies.stream()
                    .map(ac -> {
                        Map<String, Object> info = new HashMap<>();
                        info.put("constituencyName", ac.getAcName());
                        info.put("stateCode", ac.getStateCode());
                        info.put("stateName", ac.getStateName());
                        info.put("vacant", ac.getCurrentMlaName() == null);
                        return info;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error searching constituencies", e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    /**
     * Android calls: GET /mlas/constituencies-by-state?stateCode=...
     * Get constituencies by state code
     */
    @GetMapping("/mlas/constituencies-by-state")
    public ResponseEntity<List<Map<String, Object>>> getConstituenciesByState(
            @RequestParam String stateCode) {

        log.info("Legacy API: /mlas/constituencies-by-state called with stateCode={}", stateCode);

        try {
            List<AssemblyConstituencyDTO> constituencies = acService.getByStateCode(stateCode);

            List<Map<String, Object>> result = constituencies.stream()
                    .map(ac -> {
                        Map<String, Object> info = new HashMap<>();
                        info.put("constituencyName", ac.getAcName());
                        info.put("stateCode", ac.getStateCode());
                        info.put("stateName", ac.getStateName());
                        info.put("vacant", ac.getCurrentMlaName() == null);
                        return info;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error getting constituencies by state", e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    private String extractLastName(String fullName) {
        if (fullName == null || fullName.isEmpty()) return null;
        String[] parts = fullName.split("\\s+");
        return parts.length > 0 ? parts[parts.length - 1] : fullName;
    }
}
