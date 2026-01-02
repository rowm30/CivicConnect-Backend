package com.civicconnect.api.dto;

import com.civicconnect.api.entity.ParliamentaryConstituency;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

@Component
public class ParliamentaryConstituencyMapper {

    public ParliamentaryConstituencyDTO toDTO(ParliamentaryConstituency entity) {
        if (entity == null) {
            return null;
        }

        ParliamentaryConstituencyDTO.ParliamentaryConstituencyDTOBuilder builder = ParliamentaryConstituencyDTO.builder()
                .id(entity.getId())
                .pcId(entity.getPcId())
                .pcName(entity.getPcName())
                .pcNameHi(entity.getPcNameHi())
                .pcNo(entity.getPcNo())
                .stateCode(entity.getStateCode())
                .stateName(entity.getStateName())
                .reservedCategory(entity.getReservedCategory())
                .wikidataQid(entity.getWikidataQid())
                .electionPhase2019(entity.getElectionPhase2019())
                .electionDate2019(entity.getElectionDate2019())
                .currentMpName(entity.getCurrentMpName())
                .currentMpParty(entity.getCurrentMpParty())
                .areaSqKm(entity.getAreaSqKm())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());

        // Extract centroid coordinates
        Point centroid = entity.getCentroid();
        if (centroid != null) {
            builder.centroidLng(centroid.getX());
            builder.centroidLat(centroid.getY());
        }

        // Extract state ID if available
        if (entity.getState() != null) {
            builder.stateId(entity.getState().getId());
        }

        return builder.build();
    }

    public ParliamentaryConstituency toEntity(ParliamentaryConstituencyDTO dto) {
        if (dto == null) {
            return null;
        }

        return ParliamentaryConstituency.builder()
                .pcId(dto.getPcId())
                .pcName(dto.getPcName())
                .pcNameHi(dto.getPcNameHi())
                .pcNo(dto.getPcNo())
                .stateCode(dto.getStateCode())
                .stateName(dto.getStateName())
                .reservedCategory(dto.getReservedCategory())
                .wikidataQid(dto.getWikidataQid())
                .electionPhase2019(dto.getElectionPhase2019())
                .electionDate2019(dto.getElectionDate2019())
                .currentMpName(dto.getCurrentMpName())
                .currentMpParty(dto.getCurrentMpParty())
                .areaSqKm(dto.getAreaSqKm())
                .build();
    }

    public void updateEntity(ParliamentaryConstituency entity, ParliamentaryConstituencyDTO dto) {
        if (dto.getPcName() != null) {
            entity.setPcName(dto.getPcName());
        }
        if (dto.getPcNameHi() != null) {
            entity.setPcNameHi(dto.getPcNameHi());
        }
        if (dto.getCurrentMpName() != null) {
            entity.setCurrentMpName(dto.getCurrentMpName());
        }
        if (dto.getCurrentMpParty() != null) {
            entity.setCurrentMpParty(dto.getCurrentMpParty());
        }
        if (dto.getReservedCategory() != null) {
            entity.setReservedCategory(dto.getReservedCategory());
        }
        if (dto.getIsActive() != null) {
            entity.setIsActive(dto.getIsActive());
        }
    }
}
