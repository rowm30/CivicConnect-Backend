package com.civicconnect.api.controller.legacy;

import com.civicconnect.api.dto.AssemblyConstituencyDTO;
import com.civicconnect.api.dto.DistrictDTO;
import com.civicconnect.api.dto.MemberOfParliamentDTO;
import com.civicconnect.api.dto.ParliamentaryConstituencyDTO;
import com.civicconnect.api.service.AssemblyConstituencyService;
import com.civicconnect.api.service.DistrictService;
import com.civicconnect.api.service.MemberOfParliamentService;
import com.civicconnect.api.service.ParliamentaryConstituencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LegacyLocationController {

    private final ParliamentaryConstituencyService pcService;
    private final AssemblyConstituencyService acService;
    private final DistrictService districtService;
    private final MemberOfParliamentService mpService;

    @GetMapping("/location/complete-info")
    public ResponseEntity<Map<String, Object>> getCompleteLocationInfo(
            @RequestParam double latitude,
            @RequestParam double longitude) {

        log.info("Legacy API: /location/complete-info lat={}, lng={}", latitude, longitude);

        Map<String, Object> response = new HashMap<>();
        response.put("latitude", latitude);
        response.put("longitude", longitude);

        ParliamentaryConstituencyDTO pc = null;

        try {
            pc = pcService.findByLocation(latitude, longitude);
            if (pc != null) {
                response.put("parliamentaryConstituency", pc.getPcName());
                response.put("parliamentaryConstituencyNumber", pc.getPcId());
                response.put("stateName", pc.getStateName());

                MemberOfParliamentDTO mp = mpService.getByConstituencyId(pc.getId());
                if (mp != null) {
                    Map<String, Object> mpInfo = new HashMap<>();
                    mpInfo.put("name", mp.getMemberName());
                    mpInfo.put("party", mp.getPartyName());
                    mpInfo.put("constituency", mp.getConstituencyName());
                    mpInfo.put("designation", "MP");
                    mpInfo.put("membershipStatus", mp.getMembershipStatus());
                    mpInfo.put("isVacant", false);
                    response.put("mp", mpInfo);
                } else if (pc.getCurrentMpName() != null) {
                    Map<String, Object> mpInfo = new HashMap<>();
                    mpInfo.put("name", pc.getCurrentMpName());
                    mpInfo.put("party", pc.getCurrentMpParty());
                    mpInfo.put("constituency", pc.getPcName());
                    mpInfo.put("designation", "MP");
                    mpInfo.put("membershipStatus", "In Office");
                    mpInfo.put("isVacant", false);
                    response.put("mp", mpInfo);
                }
            }
        } catch (Exception e) {
            log.warn("Could not find PC: {}", e.getMessage());
        }

        try {
            AssemblyConstituencyDTO ac = acService.findByLocation(latitude, longitude);
            if (ac != null) {
                response.put("assemblyConstituency", ac.getAcName());
                response.put("assemblyConstituencyNumber", ac.getAcId());

                if (ac.getCurrentMlaName() != null) {
                    Map<String, Object> mlaInfo = new HashMap<>();
                    mlaInfo.put("name", ac.getCurrentMlaName());
                    mlaInfo.put("party", ac.getCurrentMlaParty());
                    mlaInfo.put("constituency", ac.getAcName());
                    mlaInfo.put("designation", "MLA");
                    mlaInfo.put("membershipStatus", "In Office");
                    mlaInfo.put("isVacant", false);
                    response.put("mla", mlaInfo);
                }
            }
        } catch (Exception e) {
            log.warn("Could not find AC: {}", e.getMessage());
        }

        try {
            Optional<DistrictDTO> districtOpt = districtService.findByLocation(latitude, longitude);
            if (districtOpt.isPresent()) {
                DistrictDTO district = districtOpt.get();
                response.put("districtName", district.getDistrictName());
                response.put("cityName", district.getDistrictName());
                response.put("countryName", "India");
                response.put("formattedAddress", district.getDistrictName() + ", " +
                        (pc != null ? pc.getStateName() : "India"));
            }
        } catch (Exception e) {
            log.warn("Could not find district: {}", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/geo-features/pc-name")
    public ResponseEntity<List<String>> getPcNameByLocation(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        try {
            ParliamentaryConstituencyDTO pc = pcService.findByLocation(latitude, longitude);
            if (pc != null) return ResponseEntity.ok(List.of(pc.getPcName()));
        } catch (Exception e) {
            log.warn("PC lookup failed: {}", e.getMessage());
        }
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/ac-features/ac-name")
    public ResponseEntity<List<String>> getAcNameByLocation(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        try {
            AssemblyConstituencyDTO ac = acService.findByLocation(latitude, longitude);
            if (ac != null) return ResponseEntity.ok(List.of(ac.getAcName()));
        } catch (Exception e) {
            log.warn("AC lookup failed: {}", e.getMessage());
        }
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/geocoding/reverse")
    public ResponseEntity<Map<String, String>> reverseGeocode(
            @RequestParam double latitude,
            @RequestParam double longitude) {

        Map<String, String> response = new HashMap<>();
        try {
            Optional<DistrictDTO> districtOpt = districtService.findByLocation(latitude, longitude);
            if (districtOpt.isPresent()) {
                response.put("cityName", districtOpt.get().getDistrictName());
                response.put("stateName", districtOpt.get().getStateName());
            }
        } catch (Exception ignored) {}

        response.put("countryName", "India");
        response.putIfAbsent("cityName", "Unknown");
        response.putIfAbsent("stateName", "Unknown");
        response.put("formattedAddress", response.get("cityName") + ", " + response.get("stateName"));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/districts/locate")
    public ResponseEntity<DistrictDTO> getDistrictByLocation(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        try {
            Optional<DistrictDTO> districtOpt = districtService.findByLocation(latitude, longitude);
            if (districtOpt.isPresent()) return ResponseEntity.ok(districtOpt.get());
        } catch (Exception e) {
            log.error("District lookup failed", e);
        }
        return ResponseEntity.notFound().build();
    }
}
