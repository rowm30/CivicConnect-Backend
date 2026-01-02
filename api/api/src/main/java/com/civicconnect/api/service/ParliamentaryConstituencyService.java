package com.civicconnect.api.service;

import com.civicconnect.api.dto.ParliamentaryConstituencyDTO;
import com.civicconnect.api.dto.ParliamentaryConstituencyMapper;
import com.civicconnect.api.entity.ParliamentaryConstituency;
import com.civicconnect.api.exception.ResourceNotFoundException;
import com.civicconnect.api.repository.ParliamentaryConstituencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ParliamentaryConstituencyService {

    private final ParliamentaryConstituencyRepository repository;
    private final ParliamentaryConstituencyMapper mapper;

    public Page<ParliamentaryConstituencyDTO> getAllConstituencies(int page, int size, String sortBy, String sortDir, String search) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ParliamentaryConstituency> constituencies;
        if (search != null && !search.trim().isEmpty()) {
            constituencies = repository.search(search.trim(), pageable);
        } else {
            constituencies = repository.findAll(pageable);
        }

        return constituencies.map(mapper::toDTO);
    }

    public List<ParliamentaryConstituencyDTO> getAllActiveConstituencies() {
        return repository.findByIsActiveTrue()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public ParliamentaryConstituencyDTO getConstituencyById(Long id) {
        ParliamentaryConstituency constituency = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parliamentary Constituency", id));
        return mapper.toDTO(constituency);
    }

    public ParliamentaryConstituencyDTO getConstituencyByPcId(String pcId) {
        ParliamentaryConstituency constituency = repository.findByPcId(pcId)
                .orElseThrow(() -> new ResourceNotFoundException("Parliamentary Constituency not found with pcId: " + pcId));
        return mapper.toDTO(constituency);
    }

    public List<ParliamentaryConstituencyDTO> getConstituenciesByStateCode(String stateCode) {
        return repository.findByStateCodeAndIsActiveTrue(stateCode)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public ParliamentaryConstituencyDTO findByLocation(double lat, double lng) {
        ParliamentaryConstituency constituency = repository.findByPoint(lat, lng)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No parliamentary constituency found at location: " + lat + ", " + lng));
        return mapper.toDTO(constituency);
    }

    @Cacheable(value = "pcGeoJson", key = "'all'")
    public String getAllAsGeoJson() {
        return repository.findAllAsGeoJson();
    }

    @Cacheable(value = "pcGeoJsonByState", key = "#stateName")
    public String getByStateNameAsGeoJson(String stateName) {
        return repository.findByStateNameAsGeoJson(stateName);
    }

    @Cacheable(value = "pcGeoJsonSimplified", key = "#tolerance")
    public String getAllAsSimplifiedGeoJson(double tolerance) {
        return repository.findAllAsSimplifiedGeoJson(tolerance);
    }

    public ParliamentaryConstituencyDTO updateConstituency(Long id, ParliamentaryConstituencyDTO dto) {
        ParliamentaryConstituency constituency = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parliamentary Constituency", id));

        mapper.updateEntity(constituency, dto);
        ParliamentaryConstituency updated = repository.save(constituency);
        return mapper.toDTO(updated);
    }

    public void deleteConstituency(Long id) {
        ParliamentaryConstituency constituency = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parliamentary Constituency", id));

        // Soft delete
        constituency.setIsActive(false);
        repository.save(constituency);
    }

    public List<String> getDistinctStateNames() {
        return repository.findDistinctStateNames();
    }

    public List<String> getDistinctStateCodes() {
        return repository.findDistinctStateCodes();
    }
}
