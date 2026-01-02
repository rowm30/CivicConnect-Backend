package com.civicconnect.api.controller;

import com.civicconnect.api.dto.MemberOfParliamentDTO;
import com.civicconnect.api.service.MemberOfParliamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members-of-parliament")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MemberOfParliamentController {

    private final MemberOfParliamentService service;

    @GetMapping
    public ResponseEntity<List<MemberOfParliamentDTO>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String stateName,
            @RequestParam(required = false) String partyName,
            @RequestParam(defaultValue = "0") int _start,
            @RequestParam(defaultValue = "10") int _end,
            @RequestParam(defaultValue = "id") String _sort,
            @RequestParam(defaultValue = "ASC") String _order
    ) {
        int page = _start / Math.max(1, _end - _start);
        int size = Math.max(1, _end - _start);

        Sort sort = Sort.by(_order.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, _sort);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MemberOfParliamentDTO> result = service.findAll(q, stateName, partyName, pageable);

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .header("Access-Control-Expose-Headers", "X-Total-Count")
                .body(result.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemberOfParliamentDTO> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/states")
    public ResponseEntity<List<String>> getStates() {
        return ResponseEntity.ok(service.findDistinctStateNames());
    }

    @GetMapping("/parties")
    public ResponseEntity<List<String>> getParties() {
        return ResponseEntity.ok(service.findDistinctPartyNames());
    }

    @GetMapping("/stats/by-party")
    public ResponseEntity<Map<String, Long>> getStatsByParty() {
        return ResponseEntity.ok(service.getPartyWiseCount());
    }

    @GetMapping("/stats/by-state")
    public ResponseEntity<Map<String, Long>> getStatsByState() {
        return ResponseEntity.ok(service.getStateWiseCount());
    }

    @GetMapping("/by-constituency/{constituencyId}")
    public ResponseEntity<MemberOfParliamentDTO> getByConstituency(@PathVariable Long constituencyId) {
        return service.findByConstituencyId(constituencyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MemberOfParliamentDTO> create(@RequestBody MemberOfParliamentDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemberOfParliamentDTO> update(
            @PathVariable Long id,
            @RequestBody MemberOfParliamentDTO dto
    ) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
