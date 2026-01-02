package com.civicconnect.api.controller;

import com.civicconnect.api.dto.AssemblyConstituencyDTO;
import com.civicconnect.api.service.AssemblyConstituencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assembly-constituencies")
@RequiredArgsConstructor
public class AssemblyConstituencyController {

    private final AssemblyConstituencyService service;

    /**
     * Get all constituencies with pagination, sorting, and search
     * Refine admin panel compatible endpoint
     */
    @GetMapping
    public ResponseEntity<List<AssemblyConstituencyDTO>> getAllConstituencies(
            @RequestParam(defaultValue = "0") int _start,
            @RequestParam(defaultValue = "10") int _end,
            @RequestParam(defaultValue = "id") String _sort,
            @RequestParam(defaultValue = "asc") String _order,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String stateCode,
            @RequestParam(required = false) String stateName
    ) {
        int page = _start / Math.max(1, _end - _start);
        int size = Math.max(1, _end - _start);

        if (stateName != null && !stateName.isEmpty()) {
            List<AssemblyConstituencyDTO> byState = service.getConstituenciesByStateName(stateName);
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(byState.size()))
                    .header("Access-Control-Expose-Headers", "X-Total-Count")
                    .body(byState);
        }

        if (stateCode != null && !stateCode.isEmpty()) {
            List<AssemblyConstituencyDTO> byState = service.getConstituenciesByStateCode(stateCode);
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(byState.size()))
                    .header("Access-Control-Expose-Headers", "X-Total-Count")
                    .body(byState);
        }

        Page<AssemblyConstituencyDTO> constituencyPage = service.getAllConstituencies(page, size, _sort, _order, q);

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(constituencyPage.getTotalElements()))
                .header("Access-Control-Expose-Headers", "X-Total-Count")
                .body(constituencyPage.getContent());
    }

    /**
     * Get all active constituencies (for dropdowns)
     */
    @GetMapping("/active")
    public ResponseEntity<List<AssemblyConstituencyDTO>> getAllActiveConstituencies() {
        return ResponseEntity.ok(service.getAllActiveConstituencies());
    }

    /**
     * Get constituency by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AssemblyConstituencyDTO> getConstituencyById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getConstituencyById(id));
    }

    /**
     * Get constituency by AC ID
     */
    @GetMapping("/ac/{acId}")
    public ResponseEntity<AssemblyConstituencyDTO> getConstituencyByAcId(@PathVariable String acId) {
        return ResponseEntity.ok(service.getConstituencyByAcId(acId));
    }

    /**
     * Get constituencies by state code
     */
    @GetMapping("/state-code/{stateCode}")
    public ResponseEntity<List<AssemblyConstituencyDTO>> getConstituenciesByStateCode(
            @PathVariable String stateCode) {
        return ResponseEntity.ok(service.getConstituenciesByStateCode(stateCode));
    }

    /**
     * Get constituencies by PC ID (assembly constituencies under a parliamentary constituency)
     */
    @GetMapping("/pc/{pcId}")
    public ResponseEntity<List<AssemblyConstituencyDTO>> getConstituenciesByPcId(
            @PathVariable String pcId) {
        return ResponseEntity.ok(service.getConstituenciesByPcId(pcId));
    }

    /**
     * Find constituency by GPS coordinates
     */
    @GetMapping("/find-by-location")
    public ResponseEntity<AssemblyConstituencyDTO> findByLocation(
            @RequestParam double lat,
            @RequestParam double lng) {
        return ResponseEntity.ok(service.findByLocation(lat, lng));
    }

    /**
     * Get all constituencies as GeoJSON FeatureCollection (for map rendering)
     */
    @GetMapping(value = "/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAllAsGeoJson() {
        return ResponseEntity.ok(service.getAllAsGeoJson());
    }

    /**
     * Get constituencies by state as GeoJSON
     */
    @GetMapping(value = "/geojson/state/{stateName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getByStateNameAsGeoJson(@PathVariable String stateName) {
        return ResponseEntity.ok(service.getByStateNameAsGeoJson(stateName));
    }

    /**
     * Get simplified GeoJSON for better performance (lower detail)
     */
    @GetMapping(value = "/geojson/simplified", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getSimplifiedGeoJson(
            @RequestParam(defaultValue = "0.01") double tolerance) {
        return ResponseEntity.ok(service.getAllAsSimplifiedGeoJson(tolerance));
    }

    /**
     * Update constituency (mainly for MLA details)
     */
    @PutMapping("/{id}")
    public ResponseEntity<AssemblyConstituencyDTO> updateConstituency(
            @PathVariable Long id,
            @Valid @RequestBody AssemblyConstituencyDTO dto) {
        return ResponseEntity.ok(service.updateConstituency(id, dto));
    }

    /**
     * Soft delete a constituency
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConstituency(@PathVariable Long id) {
        service.deleteConstituency(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get distinct state names (for filters)
     */
    @GetMapping("/states")
    public ResponseEntity<List<String>> getDistinctStateNames() {
        return ResponseEntity.ok(service.getDistinctStateNames());
    }

    /**
     * Get distinct state codes (for filters)
     */
    @GetMapping("/state-codes")
    public ResponseEntity<List<String>> getDistinctStateCodes() {
        return ResponseEntity.ok(service.getDistinctStateCodes());
    }

    /**
     * Get distinct districts by state (for filters)
     */
    @GetMapping("/districts/{stateName}")
    public ResponseEntity<List<String>> getDistinctDistrictsByState(@PathVariable String stateName) {
        return ResponseEntity.ok(service.getDistinctDistrictsByState(stateName));
    }
}
