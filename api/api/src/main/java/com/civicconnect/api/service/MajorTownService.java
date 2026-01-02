package com.civicconnect.api.service;

import com.civicconnect.api.dto.MajorTownDTO;
import com.civicconnect.api.entity.MajorTown;
import com.civicconnect.api.repository.MajorTownRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MajorTownService {

    private final MajorTownRepository repository;

    public Page<MajorTownDTO> findAll(String q, String stateName, String districtName, Pageable pageable) {
        Page<MajorTown> page = repository.searchTowns(q, stateName, districtName, pageable);
        return page.map(this::toDTO);
    }

    public Optional<MajorTownDTO> findById(Long id) {
        return repository.findById(id).map(this::toDTO);
    }

    public List<String> findDistinctStateNames() {
        return repository.findDistinctStateNames();
    }

    public List<String> findDistinctDistrictsByState(String stateName) {
        return repository.findDistinctDistrictsByState(stateName);
    }

    public String getGeoJsonByState(String stateName) {
        return repository.findByStateAsGeoJson(stateName);
    }

    public String getGeoJsonByDistrict(String districtName) {
        return repository.findByDistrictAsGeoJson(districtName);
    }

    private MajorTownDTO toDTO(MajorTown entity) {
        return MajorTownDTO.builder()
                .id(entity.getId())
                .townId(entity.getTownId())
                .townName(entity.getTownName())
                .districtName(entity.getDistrictName())
                .stateName(entity.getStateName())
                .elevation(entity.getElevation())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .build();
    }
}
