package com.civicconnect.api.dto;

import com.civicconnect.api.entity.State;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateDTO {

    private Long id;

    @NotBlank(message = "State name is required")
    @Size(max = 100)
    private String name;

    private String nameLocal;

    @NotBlank(message = "State code is required")
    @Size(max = 10)
    private String code;

    private String isoCode;

    private State.StateType stateType;

    private String capital;

    private String largestCity;

    private String officialLanguages;

    private Integer totalDistricts;

    private Integer totalLokSabhaSeats;

    private Integer totalVidhanSabhaSeats;

    private Boolean hasLegislativeCouncil;

    private Integer totalVidhanParishadSeats;

    private Double areaSqKm;

    private Long population;

    private Integer censusYear;

    private String officialWebsite;

    private String cmGrievancePortal;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}