package com.civicconnect.api.service;

import com.civicconnect.api.dto.StateBoundaryDTO;
import com.civicconnect.api.entity.StateBoundary;
import com.civicconnect.api.repository.StateBoundaryRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StateBoundaryService {

    private final StateBoundaryRepository repository;

    public Page<StateBoundaryDTO> findAll(Pageable pageable) {
        Page<StateBoundary> page = repository.findAll(pageable);
        return page.map(this::toDTO);
    }

    public Optional<StateBoundaryDTO> findById(Long id) {
        return repository.findById(id).map(this::toDTO);
    }

    public Optional<StateBoundaryDTO> findByStateName(String stateName) {
        return repository.findByStateName(stateName).map(this::toDTO);
    }

    public List<String> findAllStateNames() {
        return repository.findAllStateNames();
    }

    public Optional<StateBoundaryDTO> findByLocation(double lat, double lng) {
        return repository.findByPoint(lat, lng).map(this::toDTO);
    }

    public String getGeoJson() {
        return repository.findAllAsGeoJson();
    }

    public String getSimplifiedGeoJson(double tolerance) {
        return repository.findAllAsSimplifiedGeoJson(tolerance);
    }

    public String getGeoJsonByState(String stateName) {
        return repository.findByStateNameAsGeoJson(stateName);
    }

    private StateBoundaryDTO toDTO(StateBoundary entity) {
        StateBoundaryDTO dto = new StateBoundaryDTO();
        dto.setId(entity.getId());
        dto.setStateId(entity.getStateId());
        dto.setStateName(entity.getStateName());
        dto.setAreaSqKm(entity.getAreaSqKm());

        if (entity.getCentroid() != null) {
            Point centroid = entity.getCentroid();
            dto.setCentroidLat(centroid.getY());
            dto.setCentroidLng(centroid.getX());
        }

        return dto;
    }
}
