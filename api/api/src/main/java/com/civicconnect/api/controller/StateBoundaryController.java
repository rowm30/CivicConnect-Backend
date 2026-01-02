package com.civicconnect.api.controller;

import com.civicconnect.api.dto.StateBoundaryDTO;
import com.civicconnect.api.service.StateBoundaryService;
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
@RequestMapping("/api/state-boundaries")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StateBoundaryController {

    private final StateBoundaryService service;

    @GetMapping
    public ResponseEntity<List<StateBoundaryDTO>> getAll(
            @RequestParam(defaultValue = "0") int _start,
            @RequestParam(defaultValue = "10") int _end,
            @RequestParam(defaultValue = "id") String _sort,
            @RequestParam(defaultValue = "ASC") String _order
    ) {
        int page = _start / Math.max(1, _end - _start);
        int size = Math.max(1, _end - _start);

        Sort sort = Sort.by(_order.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, _sort);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<StateBoundaryDTO> result = service.findAll(pageable);

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .header("Access-Control-Expose-Headers", "X-Total-Count")
                .body(result.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StateBoundaryDTO> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/names")
    public ResponseEntity<List<String>> getStateNames() {
        return ResponseEntity.ok(service.findAllStateNames());
    }

    @GetMapping("/find-by-location")
    public ResponseEntity<StateBoundaryDTO> findByLocation(
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        return service.findByLocation(lat, lng)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getGeoJson() {
        return ResponseEntity.ok(service.getGeoJson());
    }

    @GetMapping(value = "/geojson/simplified", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getSimplifiedGeoJson(
            @RequestParam(defaultValue = "0.01") double tolerance
    ) {
        return ResponseEntity.ok(service.getSimplifiedGeoJson(tolerance));
    }

    @GetMapping(value = "/geojson/state/{stateName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getGeoJsonByState(@PathVariable String stateName) {
        return ResponseEntity.ok(service.getGeoJsonByState(stateName));
    }
}
