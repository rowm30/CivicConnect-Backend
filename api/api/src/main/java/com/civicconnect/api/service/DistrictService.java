package com.civicconnect.api.service;

import com.civicconnect.api.dto.DistrictDTO;
import com.civicconnect.api.dto.DistrictMapper;
import com.civicconnect.api.entity.District;
import com.civicconnect.api.repository.DistrictRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DistrictService {

    private final DistrictRepository districtRepository;
    private final DistrictMapper districtMapper;

    public Page<DistrictDTO> findAll(String q, String stateName, Pageable pageable) {
        Page<District> page = districtRepository.searchDistricts(q, stateName, pageable);
        return page.map(districtMapper::toDTO);
    }

    public Optional<DistrictDTO> findById(Long id) {
        return districtRepository.findById(id).map(districtMapper::toDTO);
    }

    public Optional<DistrictDTO> findByDistrictId(String districtId) {
        return districtRepository.findByDistrictId(districtId).map(districtMapper::toDTO);
    }

    public List<String> findDistinctStateNames() {
        return districtRepository.findDistinctStateNames();
    }

    public Optional<DistrictDTO> findByLocation(double lat, double lng) {
        return districtRepository.findByPoint(lat, lng).map(districtMapper::toDTO);
    }

    public String getGeoJson() {
        return districtRepository.findAllAsGeoJson();
    }

    public String getGeoJsonByState(String stateName) {
        return districtRepository.findByStateNameAsGeoJson(stateName);
    }

    public String getSimplifiedGeoJson(double tolerance) {
        return districtRepository.findAllAsSimplifiedGeoJson(tolerance);
    }

    @Transactional
    public DistrictDTO update(Long id, DistrictDTO dto) {
        District district = districtRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("District not found: " + id));

        districtMapper.updateEntity(district, dto);
        District saved = districtRepository.save(district);
        return districtMapper.toDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        districtRepository.deleteById(id);
    }
}
