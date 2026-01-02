package com.civicconnect.api.service;

import com.civicconnect.api.dto.AssemblyConstituencyDTO;
import com.civicconnect.api.dto.AssemblyConstituencyMapper;
import com.civicconnect.api.entity.AssemblyConstituency;
import com.civicconnect.api.repository.AssemblyConstituencyRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
public class AssemblyConstituencyService {

    private final AssemblyConstituencyRepository repository;
    private final AssemblyConstituencyMapper mapper;

    public Page<AssemblyConstituencyDTO> getAllConstituencies(int page, int size, String sortBy, String sortOrder, String search) {
        Sort sort = sortOrder.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AssemblyConstituency> constituencyPage;
        if (search != null && !search.isEmpty()) {
            constituencyPage = repository.search(search, pageable);
        } else {
            constituencyPage = repository.findAll(pageable);
        }

        return constituencyPage.map(mapper::toDTO);
    }

    public List<AssemblyConstituencyDTO> getAllActiveConstituencies() {
        return repository.findByIsActiveTrue().stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public AssemblyConstituencyDTO getConstituencyById(Long id) {
        AssemblyConstituency constituency = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Assembly constituency not found with id: " + id));
        return mapper.toDTO(constituency);
    }

    public AssemblyConstituencyDTO getConstituencyByAcId(String acId) {
        AssemblyConstituency constituency = repository.findByAcId(acId)
                .orElseThrow(() -> new EntityNotFoundException("Assembly constituency not found with acId: " + acId));
        return mapper.toDTO(constituency);
    }

    public List<AssemblyConstituencyDTO> getConstituenciesByStateCode(String stateCode) {
        return repository.findByStateCode(stateCode).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<AssemblyConstituencyDTO> getConstituenciesByStateName(String stateName) {
        return repository.findByStateName(stateName).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<AssemblyConstituencyDTO> getConstituenciesByPcId(String pcId) {
        return repository.findByPcId(pcId).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public AssemblyConstituencyDTO findByLocation(double lat, double lng) {
        AssemblyConstituency constituency = repository.findByPoint(lat, lng)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No assembly constituency found at location: " + lat + ", " + lng));
        return mapper.toDTO(constituency);
    }

    public String getAllAsGeoJson() {
        return repository.findAllAsGeoJson();
    }

    public String getByStateNameAsGeoJson(String stateName) {
        return repository.findByStateNameAsGeoJson(stateName);
    }

    public String getAllAsSimplifiedGeoJson(double tolerance) {
        return repository.findAllAsSimplifiedGeoJson(tolerance);
    }

    @Transactional
    public AssemblyConstituencyDTO updateConstituency(Long id, AssemblyConstituencyDTO dto) {
        AssemblyConstituency constituency = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Assembly constituency not found with id: " + id));

        mapper.updateEntity(constituency, dto);
        AssemblyConstituency updated = repository.save(constituency);
        return mapper.toDTO(updated);
    }

    @Transactional
    public void deleteConstituency(Long id) {
        AssemblyConstituency constituency = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Assembly constituency not found with id: " + id));
        constituency.setIsActive(false);
        repository.save(constituency);
    }

    public List<String> getDistinctStateNames() {
        return repository.findDistinctStateNames();
    }

    public List<String> getDistinctStateCodes() {
        return repository.findDistinctStateCodes();
    }

    public List<String> getDistinctDistrictsByState(String stateName) {
        return repository.findDistinctDistrictsByState(stateName);
    }

    public List<AssemblyConstituencyDTO> searchByName(String name) {
        return repository.findByAcNameContainingIgnoreCaseAndIsActiveTrue(name).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<AssemblyConstituencyDTO> getByStateCode(String stateCode) {
        return repository.findByStateCodeAndIsActiveTrue(stateCode).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }
}
