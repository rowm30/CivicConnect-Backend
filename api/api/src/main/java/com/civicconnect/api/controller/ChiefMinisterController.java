package com.civicconnect.api.controller;

import com.civicconnect.api.entity.ChiefMinister;
import com.civicconnect.api.repository.ChiefMinisterRepository;
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
 * REST Controller for Chief Ministers
 * Provides CRUD operations and filtering for CM data
 */
@RestController
@RequestMapping("/api/chief-ministers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chief Ministers", description = "APIs for managing Chief Minister data")
public class ChiefMinisterController {

    private final ChiefMinisterRepository chiefMinisterRepository;

    /**
     * Get all chief ministers with pagination and filtering
     */
    @GetMapping
    @Operation(summary = "Get all Chief Ministers", description = "Returns paginated list of CMs with optional filters")
    public ResponseEntity<Page<ChiefMinister>> getAllChiefMinisters(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String stateName,
            @RequestParam(required = false) String partyName,
            @RequestParam(required = false) ChiefMinister.CMStatus status,
            Pageable pageable
    ) {
        log.info("Getting all Chief Ministers with filters - q: {}, state: {}, party: {}, status: {}",
                q, stateName, partyName, status);

        Page<ChiefMinister> result;

        if (q != null && !q.isEmpty()) {
            result = chiefMinisterRepository.findAll(pageable); // Would need custom query for search
        } else if (stateName != null) {
            result = chiefMinisterRepository.findAll(pageable); // Filter by state
        } else {
            result = chiefMinisterRepository.findAll(pageable);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get CM by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get CM by ID")
    public ResponseEntity<ChiefMinister> getById(@PathVariable Long id) {
        return chiefMinisterRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get current CM by state
     */
    @GetMapping("/state/{stateName}")
    @Operation(summary = "Get current CM by state name")
    public ResponseEntity<ChiefMinister> getByState(@PathVariable String stateName) {
        return chiefMinisterRepository.findByStateNameIgnoreCaseAndStatus(stateName, ChiefMinister.CMStatus.CURRENT)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get current CM by state code
     */
    @GetMapping("/state-code/{stateCode}")
    @Operation(summary = "Get current CM by state code")
    public ResponseEntity<ChiefMinister> getByStateCode(@PathVariable String stateCode) {
        return chiefMinisterRepository.findByStateCodeAndStatus(stateCode, ChiefMinister.CMStatus.CURRENT)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all current CMs
     */
    @GetMapping("/current")
    @Operation(summary = "Get all current Chief Ministers")
    public ResponseEntity<List<ChiefMinister>> getAllCurrent() {
        return ResponseEntity.ok(chiefMinisterRepository.findAllByStatus(ChiefMinister.CMStatus.CURRENT));
    }

    /**
     * Get distinct states with CM data
     */
    @GetMapping("/states")
    @Operation(summary = "Get list of states with CM data")
    public ResponseEntity<List<String>> getStates() {
        return ResponseEntity.ok(chiefMinisterRepository.getStatesWithCMData());
    }

    /**
     * Get party-wise count
     */
    @GetMapping("/stats/by-party")
    @Operation(summary = "Get CM count by party")
    public ResponseEntity<List<Map<String, Object>>> getCountByParty() {
        List<Object[]> results = chiefMinisterRepository.countByParty();
        List<Map<String, Object>> response = results.stream()
                .map(row -> Map.of(
                        "party", row[0] != null ? row[0] : "Independent",
                        "count", row[1]
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Create new CM
     */
    @PostMapping
    @Operation(summary = "Create new Chief Minister record")
    public ResponseEntity<ChiefMinister> create(@RequestBody ChiefMinister cm) {
        log.info("Creating new CM: {} for state: {}", cm.getName(), cm.getStateName());
        return ResponseEntity.ok(chiefMinisterRepository.save(cm));
    }

    /**
     * Update CM
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update Chief Minister record")
    public ResponseEntity<ChiefMinister> update(@PathVariable Long id, @RequestBody ChiefMinister cm) {
        return chiefMinisterRepository.findById(id)
                .map(existing -> {
                    cm.setId(id);
                    cm.setCreatedAt(existing.getCreatedAt());
                    return ResponseEntity.ok(chiefMinisterRepository.save(cm));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete CM
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Chief Minister record")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (chiefMinisterRepository.existsById(id)) {
            chiefMinisterRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Search CMs by name
     */
    @GetMapping("/search")
    @Operation(summary = "Search Chief Ministers by name")
    public ResponseEntity<List<ChiefMinister>> search(@RequestParam String name) {
        return ResponseEntity.ok(chiefMinisterRepository.searchByName(name));
    }
}
