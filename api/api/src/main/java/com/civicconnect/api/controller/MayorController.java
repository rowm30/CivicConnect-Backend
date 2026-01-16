package com.civicconnect.api.controller;

import com.civicconnect.api.entity.Mayor;
import com.civicconnect.api.repository.MayorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for Mayors / Municipal Commissioners
 * Provides CRUD operations and filtering for Mayor data
 */
@RestController
@RequestMapping("/api/mayors")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mayors", description = "APIs for managing Mayor / Municipal Commissioner data")
public class MayorController {

    private final MayorRepository mayorRepository;

    /**
     * Get all mayors with pagination and filtering
     */
    @GetMapping
    @Operation(summary = "Get all Mayors", description = "Returns paginated list of Mayors with optional filters")
    public ResponseEntity<Page<Mayor>> getAllMayors(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String stateName,
            @RequestParam(required = false) String cityName,
            @RequestParam(required = false) Mayor.MayorStatus status,
            Pageable pageable
    ) {
        log.info("Getting all Mayors with filters - q: {}, state: {}, city: {}, status: {}",
                q, stateName, cityName, status);

        Page<Mayor> result = mayorRepository.findAll(pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Get Mayor by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Mayor by ID")
    public ResponseEntity<Mayor> getById(@PathVariable Long id) {
        return mayorRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get current Mayor by city
     */
    @GetMapping("/city/{cityName}")
    @Operation(summary = "Get current Mayor by city name")
    public ResponseEntity<Mayor> getByCity(@PathVariable String cityName) {
        return mayorRepository.findByCityNameIgnoreCaseAndStatus(cityName, Mayor.MayorStatus.CURRENT)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get current Mayor by city and state
     */
    @GetMapping("/city/{cityName}/state/{stateName}")
    @Operation(summary = "Get current Mayor by city and state")
    public ResponseEntity<Mayor> getByCityAndState(
            @PathVariable String cityName,
            @PathVariable String stateName
    ) {
        return mayorRepository.findByCityAndStateAndStatus(cityName, stateName, Mayor.MayorStatus.CURRENT)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all current Mayors
     */
    @GetMapping("/current")
    @Operation(summary = "Get all current Mayors")
    public ResponseEntity<List<Mayor>> getAllCurrent() {
        return ResponseEntity.ok(mayorRepository.findAllByStatus(Mayor.MayorStatus.CURRENT));
    }

    /**
     * Get Mayors by state
     */
    @GetMapping("/state/{stateName}")
    @Operation(summary = "Get all Mayors in a state")
    public ResponseEntity<List<Mayor>> getByState(@PathVariable String stateName) {
        return ResponseEntity.ok(mayorRepository.findByStateNameIgnoreCaseAndStatus(stateName, Mayor.MayorStatus.CURRENT));
    }

    /**
     * Get distinct cities with Mayor data
     */
    @GetMapping("/cities")
    @Operation(summary = "Get list of cities with Mayor data")
    public ResponseEntity<List<String>> getCities() {
        return ResponseEntity.ok(mayorRepository.getCitiesWithMayorData());
    }

    /**
     * Get distinct states with Mayor data
     */
    @GetMapping("/states")
    @Operation(summary = "Get list of states with Mayor data")
    public ResponseEntity<List<String>> getStates() {
        return ResponseEntity.ok(mayorRepository.getStatesWithMayorData());
    }

    /**
     * Get count by municipal body type
     */
    @GetMapping("/stats/by-type")
    @Operation(summary = "Get Mayor count by municipal body type")
    public ResponseEntity<List<Map<String, Object>>> getCountByType() {
        List<Object[]> results = mayorRepository.countByMunicipalBodyType();
        List<Map<String, Object>> response = results.stream()
                .map(row -> Map.of(
                        "type", row[0].toString(),
                        "count", row[1]
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get count by state
     */
    @GetMapping("/stats/by-state")
    @Operation(summary = "Get Mayor count by state")
    public ResponseEntity<List<Map<String, Object>>> getCountByState() {
        List<Object[]> results = mayorRepository.countByState();
        List<Map<String, Object>> response = results.stream()
                .map(row -> Map.of(
                        "state", row[0],
                        "count", row[1]
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Search Mayors by city or name
     */
    @GetMapping("/search")
    @Operation(summary = "Search Mayors by city or name")
    public ResponseEntity<List<Mayor>> search(@RequestParam String q) {
        return ResponseEntity.ok(mayorRepository.searchByCityOrName(q));
    }

    /**
     * Create new Mayor
     */
    @PostMapping
    @Operation(summary = "Create new Mayor record")
    public ResponseEntity<Mayor> create(@RequestBody Mayor mayor) {
        log.info("Creating new Mayor: {} for city: {}", mayor.getName(), mayor.getCityName());
        return ResponseEntity.ok(mayorRepository.save(mayor));
    }

    /**
     * Update Mayor
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update Mayor record")
    public ResponseEntity<Mayor> update(@PathVariable Long id, @RequestBody Mayor mayor) {
        return mayorRepository.findById(id)
                .map(existing -> {
                    mayor.setId(id);
                    mayor.setCreatedAt(existing.getCreatedAt());
                    return ResponseEntity.ok(mayorRepository.save(mayor));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete Mayor
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Mayor record")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (mayorRepository.existsById(id)) {
            mayorRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
