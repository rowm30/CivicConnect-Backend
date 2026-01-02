package com.civicconnect.api.service;

import com.civicconnect.api.dto.DistrictHeadquarterDTO;
import com.civicconnect.api.entity.DistrictHeadquarter;
import com.civicconnect.api.repository.DistrictHeadquarterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DistrictHeadquarterService {

    private final DistrictHeadquarterRepository repository;

    public Page<DistrictHeadquarterDTO> findAll(String q, String stateName, Pageable pageable) {
        Page<DistrictHeadquarter> page = repository.searchHQs(q, stateName, pageable);
        return page.map(this::toDTO);
    }

    public Optional<DistrictHeadquarterDTO> findById(Long id) {
        return repository.findById(id).map(this::toDTO);
    }

    public List<String> findDistinctStateNames() {
        return repository.findDistinctStateNames();
    }

    public String getGeoJson() {
        return repository.findAllAsGeoJson();
    }

    public String getGeoJsonByState(String stateName) {
        return repository.findByStateAsGeoJson(stateName);
    }

    private DistrictHeadquarterDTO toDTO(DistrictHeadquarter entity) {
        return DistrictHeadquarterDTO.builder()
                .id(entity.getId())
                .hqId(entity.getHqId())
                .hqName(entity.getHqName())
                .townName(entity.getTownName())
                .districtName(entity.getDistrictName())
                .stateName(entity.getStateName())
                .talukName(entity.getTalukName())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .build();
    }
}
