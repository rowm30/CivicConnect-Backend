package com.civicconnect.api.dto;

import com.civicconnect.api.entity.District;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

@Component
public class DistrictMapper {

    public DistrictDTO toDTO(District entity) {
        if (entity == null) return null;

        DistrictDTO dto = new DistrictDTO();
        dto.setId(entity.getId());
        dto.setDistrictId(entity.getDistrictId());
        dto.setDistrictName(entity.getDistrictName());
        dto.setStateName(entity.getStateName());
        dto.setStateLgd(entity.getStateLgd());
        dto.setDistLgd(entity.getDistLgd());
        dto.setRemarks(entity.getRemarks());
        dto.setAreaSqKm(entity.getAreaSqKm());

        if (entity.getCentroid() != null) {
            Point centroid = entity.getCentroid();
            dto.setCentroidLat(centroid.getY());
            dto.setCentroidLng(centroid.getX());
        }

        return dto;
    }

    public District toEntity(DistrictDTO dto) {
        if (dto == null) return null;

        District entity = new District();
        entity.setId(dto.getId());
        entity.setDistrictId(dto.getDistrictId());
        entity.setDistrictName(dto.getDistrictName());
        entity.setStateName(dto.getStateName());
        entity.setStateLgd(dto.getStateLgd());
        entity.setDistLgd(dto.getDistLgd());
        entity.setRemarks(dto.getRemarks());
        entity.setAreaSqKm(dto.getAreaSqKm());

        return entity;
    }

    public void updateEntity(District entity, DistrictDTO dto) {
        if (dto.getRemarks() != null) {
            entity.setRemarks(dto.getRemarks());
        }
    }
}
