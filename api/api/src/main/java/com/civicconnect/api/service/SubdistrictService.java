package com.civicconnect.api.service;

import com.civicconnect.api.dto.SubdistrictDTO;
import com.civicconnect.api.entity.Subdistrict;
import com.civicconnect.api.repository.SubdistrictRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubdistrictService {

    private final SubdistrictRepository repository;

    public Page<SubdistrictDTO> findAll(String q, String stateName, String districtName, Pageable pageable) {
        Page<Subdistrict> page = repository.searchSubdistricts(q, stateName, districtName, pageable);
        return page.map(this::toDTO);
    }

    public Optional<SubdistrictDTO> findById(Long id) {
        return repository.findById(id).map(this::toDTO);
    }

    public List<String> findDistinctStateNames() {
        return repository.findDistinctStateNames();
    }

    public List<String> findDistinctDistrictsByState(String stateName) {
        return repository.findDistinctDistrictsByState(stateName);
    }

    public Optional<SubdistrictDTO> findByLocation(double lat, double lng) {
        return repository.findByPoint(lat, lng).map(this::toDTO);
    }

    public String getGeoJsonByDistrict(String districtName) {
        return repository.findByDistrictAsGeoJson(districtName);
    }

    public String getGeoJsonByState(String stateName) {
        return repository.findByStateAsGeoJson(stateName);
    }

    private SubdistrictDTO toDTO(Subdistrict entity) {
        SubdistrictDTO dto = new SubdistrictDTO();
        dto.setId(entity.getId());
        dto.setSubdistrictId(entity.getSubdistrictId());
        dto.setSubdistrictName(entity.getSubdistrictName());
        dto.setSubdistrictType(entity.getSubdistrictType());
        dto.setDistrictName(entity.getDistrictName());
        dto.setStateName(entity.getStateName());
        dto.setStateLgd(entity.getStateLgd());
        dto.setDistLgd(entity.getDistLgd());
        dto.setSubdisLgd(entity.getSubdisLgd());
        dto.setRemarks(entity.getRemarks());
        dto.setAreaSqKm(entity.getAreaSqKm());

        if (entity.getCentroid() != null) {
            Point centroid = entity.getCentroid();
            dto.setCentroidLat(centroid.getY());
            dto.setCentroidLng(centroid.getX());
        }

        return dto;
    }
}
