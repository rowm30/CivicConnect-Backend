package com.civicconnect.api.controller;

import com.civicconnect.api.dto.MajorTownDTO;
import com.civicconnect.api.service.MajorTownService;
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
@RequestMapping("/api/major-towns")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MajorTownController {

    private final MajorTownService service;

    @GetMapping
    public ResponseEntity<List<MajorTownDTO>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String stateName,
            @RequestParam(required = false) String districtName,
            @RequestParam(defaultValue = "0") int _start,
            @RequestParam(defaultValue = "10") int _end,
            @RequestParam(defaultValue = "id") String _sort,
            @RequestParam(defaultValue = "ASC") String _order
    ) {
        int page = _start / Math.max(1, _end - _start);
        int size = Math.max(1, _end - _start);

        Sort sort = Sort.by(_order.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, _sort);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MajorTownDTO> result = service.findAll(q, stateName, districtName, pageable);

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .header("Access-Control-Expose-Headers", "X-Total-Count")
                .body(result.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MajorTownDTO> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/states")
    public ResponseEntity<List<String>> getStates() {
        return ResponseEntity.ok(service.findDistinctStateNames());
    }

    @GetMapping("/districts")
    public ResponseEntity<List<String>> getDistrictsByState(@RequestParam String stateName) {
        return ResponseEntity.ok(service.findDistinctDistrictsByState(stateName));
    }

    @GetMapping(value = "/geojson/state/{stateName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getGeoJsonByState(@PathVariable String stateName) {
        return ResponseEntity.ok(service.getGeoJsonByState(stateName));
    }

    @GetMapping(value = "/geojson/district/{districtName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getGeoJsonByDistrict(@PathVariable String districtName) {
        return ResponseEntity.ok(service.getGeoJsonByDistrict(districtName));
    }
}
