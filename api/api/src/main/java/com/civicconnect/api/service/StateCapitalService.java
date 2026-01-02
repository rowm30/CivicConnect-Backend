package com.civicconnect.api.service;

import com.civicconnect.api.dto.StateCapitalDTO;
import com.civicconnect.api.entity.StateCapital;
import com.civicconnect.api.repository.StateCapitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StateCapitalService {

    private final StateCapitalRepository repository;

    public Page<StateCapitalDTO> findAll(Pageable pageable) {
        Page<StateCapital> page = repository.findAll(pageable);
        return page.map(this::toDTO);
    }

    public Optional<StateCapitalDTO> findById(Long id) {
        return repository.findById(id).map(this::toDTO);
    }

    public Optional<StateCapitalDTO> findByStateName(String stateName) {
        return repository.findByStateName(stateName).map(this::toDTO);
    }

    public String getGeoJson() {
        return repository.findAllAsGeoJson();
    }

    private StateCapitalDTO toDTO(StateCapital entity) {
        return StateCapitalDTO.builder()
                .id(entity.getId())
                .capitalId(entity.getCapitalId())
                .stateName(entity.getStateName())
                .capitalName(entity.getCapitalName())
                .stateNo(entity.getStateNo())
                .elevation(entity.getElevation())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .build();
    }
}
