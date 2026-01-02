package com.civicconnect.api.dto;

import com.civicconnect.api.entity.AssemblyConstituency;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

@Component
public class AssemblyConstituencyMapper {

    public AssemblyConstituencyDTO toDTO(AssemblyConstituency entity) {
        if (entity == null) {
            return null;
        }

        Double centroidLat = null;
        Double centroidLng = null;
        if (entity.getCentroid() != null) {
            Point centroid = entity.getCentroid();
            centroidLat = centroid.getY();
            centroidLng = centroid.getX();
        }

        return AssemblyConstituencyDTO.builder()
                .id(entity.getId())
                .acId(entity.getAcId())
                .acName(entity.getAcName())
                .acNo(entity.getAcNo())
                .stateCode(entity.getStateCode())
                .stateName(entity.getStateName())
                .districtCode(entity.getDistrictCode())
                .districtName(entity.getDistrictName())
                .pcNo(entity.getPcNo())
                .pcName(entity.getPcName())
                .pcId(entity.getPcId())
                .reservedCategory(entity.getReservedCategory())
                .status(entity.getStatus())
                .currentMlaName(entity.getCurrentMlaName())
                .currentMlaParty(entity.getCurrentMlaParty())
                .areaSqKm(entity.getAreaSqKm())
                .centroidLat(centroidLat)
                .centroidLng(centroidLng)
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntity(AssemblyConstituency entity, AssemblyConstituencyDTO dto) {
        if (dto.getCurrentMlaName() != null) {
            entity.setCurrentMlaName(dto.getCurrentMlaName());
        }
        if (dto.getCurrentMlaParty() != null) {
            entity.setCurrentMlaParty(dto.getCurrentMlaParty());
        }
        if (dto.getIsActive() != null) {
            entity.setIsActive(dto.getIsActive());
        }
    }
}
