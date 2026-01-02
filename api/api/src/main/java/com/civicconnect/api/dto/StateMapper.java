package com.civicconnect.api.dto;

import com.civicconnect.api.entity.State;
import org.springframework.stereotype.Component;

@Component
public class StateMapper {

    public StateDTO toDTO(State state) {
        if (state == null) return null;

        return StateDTO.builder()
                .id(state.getId())
                .name(state.getName())
                .nameLocal(state.getNameLocal())
                .code(state.getCode())
                .isoCode(state.getIsoCode())
                .stateType(state.getStateType())
                .capital(state.getCapital())
                .largestCity(state.getLargestCity())
                .officialLanguages(state.getOfficialLanguages())
                .totalDistricts(state.getTotalDistricts())
                .totalLokSabhaSeats(state.getTotalLokSabhaSeats())
                .totalVidhanSabhaSeats(state.getTotalVidhanSabhaSeats())
                .hasLegislativeCouncil(state.getHasLegislativeCouncil())
                .totalVidhanParishadSeats(state.getTotalVidhanParishadSeats())
                .areaSqKm(state.getAreaSqKm())
                .population(state.getPopulation())
                .censusYear(state.getCensusYear())
                .officialWebsite(state.getOfficialWebsite())
                .cmGrievancePortal(state.getCmGrievancePortal())
                .isActive(state.getIsActive())
                .createdAt(state.getCreatedAt())
                .updatedAt(state.getUpdatedAt())
                .build();
    }

    public State toEntity(StateDTO dto) {
        if (dto == null) return null;

        return State.builder()
                .name(dto.getName())
                .nameLocal(dto.getNameLocal())
                .code(dto.getCode())
                .isoCode(dto.getIsoCode())
                .stateType(dto.getStateType())
                .capital(dto.getCapital())
                .largestCity(dto.getLargestCity())
                .officialLanguages(dto.getOfficialLanguages())
                .totalDistricts(dto.getTotalDistricts())
                .totalLokSabhaSeats(dto.getTotalLokSabhaSeats())
                .totalVidhanSabhaSeats(dto.getTotalVidhanSabhaSeats())
                .hasLegislativeCouncil(dto.getHasLegislativeCouncil())
                .totalVidhanParishadSeats(dto.getTotalVidhanParishadSeats())
                .areaSqKm(dto.getAreaSqKm())
                .population(dto.getPopulation())
                .censusYear(dto.getCensusYear())
                .officialWebsite(dto.getOfficialWebsite())
                .cmGrievancePortal(dto.getCmGrievancePortal())
                .build();
    }

    public void updateEntity(State state, StateDTO dto) {
        if (dto.getName() != null) state.setName(dto.getName());
        if (dto.getNameLocal() != null) state.setNameLocal(dto.getNameLocal());
        if (dto.getCode() != null) state.setCode(dto.getCode());
        if (dto.getIsoCode() != null) state.setIsoCode(dto.getIsoCode());
        if (dto.getStateType() != null) state.setStateType(dto.getStateType());
        if (dto.getCapital() != null) state.setCapital(dto.getCapital());
        if (dto.getLargestCity() != null) state.setLargestCity(dto.getLargestCity());
        if (dto.getOfficialLanguages() != null) state.setOfficialLanguages(dto.getOfficialLanguages());
        if (dto.getTotalDistricts() != null) state.setTotalDistricts(dto.getTotalDistricts());
        if (dto.getTotalLokSabhaSeats() != null) state.setTotalLokSabhaSeats(dto.getTotalLokSabhaSeats());
        if (dto.getTotalVidhanSabhaSeats() != null) state.setTotalVidhanSabhaSeats(dto.getTotalVidhanSabhaSeats());
        if (dto.getHasLegislativeCouncil() != null) state.setHasLegislativeCouncil(dto.getHasLegislativeCouncil());
        if (dto.getTotalVidhanParishadSeats() != null) state.setTotalVidhanParishadSeats(dto.getTotalVidhanParishadSeats());
        if (dto.getAreaSqKm() != null) state.setAreaSqKm(dto.getAreaSqKm());
        if (dto.getPopulation() != null) state.setPopulation(dto.getPopulation());
        if (dto.getCensusYear() != null) state.setCensusYear(dto.getCensusYear());
        if (dto.getOfficialWebsite() != null) state.setOfficialWebsite(dto.getOfficialWebsite());
        if (dto.getCmGrievancePortal() != null) state.setCmGrievancePortal(dto.getCmGrievancePortal());
        if (dto.getIsActive() != null) state.setIsActive(dto.getIsActive());
    }
}