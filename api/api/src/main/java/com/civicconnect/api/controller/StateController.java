package com.civicconnect.api.controller;

import com.civicconnect.api.dto.StateDTO;
import com.civicconnect.api.entity.State;
import com.civicconnect.api.service.StateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/states")
@RequiredArgsConstructor
public class StateController {

    private final StateService stateService;

    /**
     * Get all states with pagination, sorting, and search
     * Refine admin panel compatible endpoint
     */
    @GetMapping
    public ResponseEntity<List<StateDTO>> getAllStates(
            @RequestParam(defaultValue = "0") int _start,
            @RequestParam(defaultValue = "10") int _end,
            @RequestParam(defaultValue = "id") String _sort,
            @RequestParam(defaultValue = "asc") String _order,
            @RequestParam(required = false) String q  // search query
    ) {
        int page = _start / (_end - _start);
        int size = _end - _start;

        Page<StateDTO> statePage = stateService.getAllStates(page, size, _sort, _order, q);

        // Refine needs X-Total-Count header for pagination
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(statePage.getTotalElements()))
                .header("Access-Control-Expose-Headers", "X-Total-Count")
                .body(statePage.getContent());
    }

    /**
     * Get all active states (for dropdowns)
     */
    @GetMapping("/active")
    public ResponseEntity<List<StateDTO>> getAllActiveStates() {
        return ResponseEntity.ok(stateService.getAllActiveStates());
    }

    /**
     * Get state by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<StateDTO> getStateById(@PathVariable Long id) {
        return ResponseEntity.ok(stateService.getStateById(id));
    }

    /**
     * Get state by code (e.g., MH, UP, KA)
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<StateDTO> getStateByCode(@PathVariable String code) {
        return ResponseEntity.ok(stateService.getStateByCode(code));
    }

    /**
     * Get states by type (STATE, UNION_TERRITORY, etc.)
     */
    @GetMapping("/type/{stateType}")
    public ResponseEntity<List<StateDTO>> getStatesByType(@PathVariable State.StateType stateType) {
        return ResponseEntity.ok(stateService.getStatesByType(stateType));
    }

    /**
     * Create a new state
     */
    @PostMapping
    public ResponseEntity<StateDTO> createState(@Valid @RequestBody StateDTO stateDTO) {
        StateDTO created = stateService.createState(stateDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing state
     */
    @PutMapping("/{id}")
    public ResponseEntity<StateDTO> updateState(
            @PathVariable Long id,
            @Valid @RequestBody StateDTO stateDTO
    ) {
        return ResponseEntity.ok(stateService.updateState(id, stateDTO));
    }

    /**
     * Soft delete a state (mark as inactive)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteState(@PathVariable Long id) {
        stateService.deleteState(id);
        return ResponseEntity.noContent().build();
    }
}