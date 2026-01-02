package com.civicconnect.api.controller;

import com.civicconnect.api.dto.DistrictDTO;
import com.civicconnect.api.service.DistrictService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/districts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DistrictController {

    private final DistrictService districtService;

    @GetMapping
    public ResponseEntity<List<DistrictDTO>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String stateName,
            @RequestParam(defaultValue = "0") int _start,
            @RequestParam(defaultValue = "10") int _end,
            @RequestParam(defaultValue = "id") String _sort,
            @RequestParam(defaultValue = "ASC") String _order
    ) {
        int page = _start / Math.max(1, _end - _start);
        int size = Math.max(1, _end - _start);

        Sort sort = Sort.by(_order.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, _sort);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DistrictDTO> result = districtService.findAll(q, stateName, pageable);

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .header("Access-Control-Expose-Headers", "X-Total-Count")
                .body(result.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DistrictDTO> getById(@PathVariable Long id) {
        return districtService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/states")
    public ResponseEntity<List<String>> getStates() {
        return ResponseEntity.ok(districtService.findDistinctStateNames());
    }

    @GetMapping("/find-by-location")
    public ResponseEntity<DistrictDTO> findByLocation(
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        return districtService.findByLocation(lat, lng)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getGeoJson() {
        return ResponseEntity.ok(districtService.getGeoJson());
    }

    @GetMapping(value = "/geojson/state/{stateName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getGeoJsonByState(@PathVariable String stateName) {
        return ResponseEntity.ok(districtService.getGeoJsonByState(stateName));
    }

    @GetMapping(value = "/geojson/simplified", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getSimplifiedGeoJson(
            @RequestParam(defaultValue = "0.01") double tolerance
    ) {
        return ResponseEntity.ok(districtService.getSimplifiedGeoJson(tolerance));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DistrictDTO> update(
            @PathVariable Long id,
            @RequestBody DistrictDTO dto
    ) {
        return ResponseEntity.ok(districtService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        districtService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
