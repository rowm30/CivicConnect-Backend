package com.civicconnect.api.controller;

import com.civicconnect.api.dto.DistrictHeadquarterDTO;
import com.civicconnect.api.service.DistrictHeadquarterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/district-headquarters")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DistrictHeadquarterController {

    private final DistrictHeadquarterService service;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String stateName,
            @RequestParam(defaultValue = "0") int _start,
            @RequestParam(defaultValue = "10") int _end,
            @RequestParam(defaultValue = "id") String _sort,
            @RequestParam(defaultValue = "ASC") String _order
    ) {
        int page = _start / (_end - _start);
        int size = _end - _start;

        Sort sort = Sort.by(_order.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, _sort);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DistrictHeadquarterDTO> result = service.findAll(q, stateName, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("data", result.getContent());
        response.put("total", result.getTotalElements());

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .header("Access-Control-Expose-Headers", "X-Total-Count")
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DistrictHeadquarterDTO> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/states")
    public ResponseEntity<List<String>> getStates() {
        return ResponseEntity.ok(service.findDistinctStateNames());
    }

    @GetMapping(value = "/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getGeoJson() {
        return ResponseEntity.ok(service.getGeoJson());
    }

    @GetMapping(value = "/geojson/state/{stateName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getGeoJsonByState(@PathVariable String stateName) {
        return ResponseEntity.ok(service.getGeoJsonByState(stateName));
    }
}
