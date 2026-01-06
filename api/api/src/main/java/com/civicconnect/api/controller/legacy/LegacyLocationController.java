package com.civicconnect.api.controller.legacy;

import com.civicconnect.api.dto.AssemblyConstituencyDTO;
import com.civicconnect.api.dto.DistrictDTO;
import com.civicconnect.api.dto.MemberOfParliamentDTO;
import com.civicconnect.api.dto.ParliamentaryConstituencyDTO;
import com.civicconnect.api.dto.WardCouncillorDTO;
import com.civicconnect.api.service.AssemblyConstituencyService;
import com.civicconnect.api.service.DistrictService;
import com.civicconnect.api.service.GeocodingService;
import com.civicconnect.api.service.MemberOfParliamentService;
import com.civicconnect.api.service.ParliamentaryConstituencyService;
import com.civicconnect.api.service.WardCouncillorService;
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
    private final WardCouncillorService wardCouncillorService;
    private final GeocodingService geocodingService;

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
                    mpInfo.put("id", mp.getId());
                    mpInfo.put("name", mp.getMemberName());
                    mpInfo.put("party", mp.getPartyName());
                    mpInfo.put("constituency", mp.getConstituencyName());
                    mpInfo.put("designation", "MP");
                    mpInfo.put("membershipStatus", mp.getMembershipStatus());
                    mpInfo.put("isVacant", false);
                    mpInfo.put("photoUrl", mp.getPhotoUrl());
                    mpInfo.put("email", mp.getEmail());
                    mpInfo.put("phone", mp.getPhone());
                    response.put("mp", mpInfo);
                } else if (pc.getCurrentMpName() != null) {
                    Map<String, Object> mpInfo = new HashMap<>();
                    mpInfo.put("id", pc.getId()); // Use PC ID as fallback for rating
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
                    mlaInfo.put("id", ac.getId()); // Use AC ID for MLA rating
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

        // Try to find ward councillor (for municipal areas like Delhi)
        try {
            String cityName = (String) response.get("cityName");
            String stateName = (String) response.get("stateName");
            // For Delhi, the city and state are the same
            String city = cityName != null ? cityName : stateName;

            if (city != null) {
                // Check if councillor data exists for this city
                List<WardCouncillorDTO> councillors = wardCouncillorService.findByCity(city);
                if (!councillors.isEmpty()) {
                    response.put("hasWardCouncillorData", true);
                    response.put("totalWardCouncillors", councillors.size());

                    // Use Google Geocoding to get locality name for precise matching
                    Optional<GeocodingService.GeocodingResult> geocodeResult =
                            geocodingService.reverseGeocode(latitude, longitude);

                    if (geocodeResult.isPresent()) {
                        GeocodingService.GeocodingResult geo = geocodeResult.get();
                        response.put("locality", geo.locality());
                        response.put("sublocality", geo.sublocality());

                        // Try to find councillor by locality match
                        Optional<WardCouncillorDTO> councillor = Optional.empty();

                        // Try locality first (neighborhood)
                        if (geo.locality() != null) {
                            councillor = wardCouncillorService.findByLocality(geo.locality(), city);
                        }

                        // Try sublocality if locality didn't match
                        if (councillor.isEmpty() && geo.sublocalityLevel1() != null) {
                            councillor = wardCouncillorService.findByLocality(geo.sublocalityLevel1(), city);
                        }

                        // Try address match as last resort
                        if (councillor.isEmpty() && geo.formattedAddress() != null) {
                            councillor = wardCouncillorService.findByAddress(geo.formattedAddress(), city);
                        }

                        // If we found a councillor, add to response
                        if (councillor.isPresent()) {
                            WardCouncillorDTO c = councillor.get();
                            Map<String, Object> councillorInfo = new HashMap<>();
                            councillorInfo.put("id", c.getId());
                            councillorInfo.put("name", c.getCouncillorName());
                            councillorInfo.put("party", c.getPartyAffiliation());
                            councillorInfo.put("wardNo", c.getWardNo());
                            councillorInfo.put("wardName", c.getWardName());
                            councillorInfo.put("designation", "Ward Councillor");
                            councillorInfo.put("phone", c.getPhone());
                            councillorInfo.put("email", c.getEmail());
                            response.put("councillor", councillorInfo);
                            log.info("Matched councillor: {} for ward {} - {}",
                                    c.getCouncillorName(), c.getWardNo(), c.getWardName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not find ward councillor: {}", e.getMessage());
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
