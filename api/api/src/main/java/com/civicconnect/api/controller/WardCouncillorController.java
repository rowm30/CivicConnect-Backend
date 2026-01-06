package com.civicconnect.api.controller;

import com.civicconnect.api.dto.WardCouncillorDTO;
import com.civicconnect.api.service.WardCouncillorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Ward Councillor data
 * Provides endpoints for mobile app and admin panel
 */
@RestController
@RequestMapping("/api/ward-councillors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class WardCouncillorController {

    private final WardCouncillorService service;

    /**
     * Get all councillors for a city
     * Example: GET /api/ward-councillors?city=Delhi
     */
    @GetMapping
    public ResponseEntity<List<WardCouncillorDTO>> getByCity(
            @RequestParam String city
    ) {
        List<WardCouncillorDTO> councillors = service.findByCity(city);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(councillors.size()))
                .header("Access-Control-Expose-Headers", "X-Total-Count")
                .body(councillors);
    }

    /**
     * Get councillor by ward number
     * Example: GET /api/ward-councillors/by-ward?wardNo=1&city=Delhi
     */
    @GetMapping("/by-ward")
    public ResponseEntity<WardCouncillorDTO> getByWardNo(
            @RequestParam Integer wardNo,
            @RequestParam String city
    ) {
        return service.findByWardNoAndCity(wardNo, city)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get councillor by ward name
     * Example: GET /api/ward-councillors/by-ward-name?wardName=Narela&city=Delhi
     */
    @GetMapping("/by-ward-name")
    public ResponseEntity<WardCouncillorDTO> getByWardName(
            @RequestParam String wardName,
            @RequestParam String city
    ) {
        return service.findByWardNameAndCity(wardName, city)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search councillors by ward name (partial match)
     * Example: GET /api/ward-councillors/search?q=nagar&city=Delhi
     */
    @GetMapping("/search")
    public ResponseEntity<List<WardCouncillorDTO>> search(
            @RequestParam String q,
            @RequestParam String city
    ) {
        List<WardCouncillorDTO> results = service.searchByWardName(q, city);
        return ResponseEntity.ok(results);
    }

    /**
     * Get all councillors for a state
     * Example: GET /api/ward-councillors/by-state?state=Delhi
     */
    @GetMapping("/by-state")
    public ResponseEntity<List<WardCouncillorDTO>> getByState(
            @RequestParam String state
    ) {
        List<WardCouncillorDTO> councillors = service.findByState(state);
        return ResponseEntity.ok(councillors);
    }

    /**
     * Get councillors by party in a city
     * Example: GET /api/ward-councillors/by-party?party=AAP&city=Delhi
     */
    @GetMapping("/by-party")
    public ResponseEntity<List<WardCouncillorDTO>> getByParty(
            @RequestParam String party,
            @RequestParam String city
    ) {
        List<WardCouncillorDTO> councillors = service.findByPartyAndCity(party, city);
        return ResponseEntity.ok(councillors);
    }

    /**
     * Get party-wise statistics for a city
     * Example: GET /api/ward-councillors/stats/by-party?city=Delhi
     */
    @GetMapping("/stats/by-party")
    public ResponseEntity<Map<String, Long>> getPartyStats(
            @RequestParam String city
    ) {
        return ResponseEntity.ok(service.getPartyWiseCount(city));
    }

    /**
     * Get total councillor count for a city
     * Example: GET /api/ward-councillors/stats/count?city=Delhi
     */
    @GetMapping("/stats/count")
    public ResponseEntity<Map<String, Object>> getCount(
            @RequestParam String city
    ) {
        long count = service.getCountByCity(city);
        return ResponseEntity.ok(Map.of(
                "city", city,
                "count", count
        ));
    }

    /**
     * Get list of all cities that have councillor data
     * Example: GET /api/ward-councillors/cities
     */
    @GetMapping("/cities")
    public ResponseEntity<List<String>> getCities() {
        return ResponseEntity.ok(service.getAllCities());
    }

    /**
     * Get list of all states that have councillor data
     * Example: GET /api/ward-councillors/states
     */
    @GetMapping("/states")
    public ResponseEntity<List<String>> getStates() {
        return ResponseEntity.ok(service.getAllStates());
    }

    /**
     * Create a new councillor (for admin)
     */
    @PostMapping
    public ResponseEntity<WardCouncillorDTO> create(@RequestBody WardCouncillorDTO dto) {
        return ResponseEntity.ok(service.save(dto));
    }

    /**
     * Bulk import councillors (for admin)
     */
    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> bulkImport(@RequestBody List<WardCouncillorDTO> dtos) {
        int count = service.saveAll(dtos);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "imported", count
        ));
    }

    /**
     * Delete councillor by ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
