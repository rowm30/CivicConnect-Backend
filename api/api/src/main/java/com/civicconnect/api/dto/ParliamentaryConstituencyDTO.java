package com.civicconnect.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParliamentaryConstituencyDTO {

    private Long id;

    @Size(max = 10)
    private String pcId;

    @NotBlank(message = "Constituency name is required")
    @Size(max = 255)
    private String pcName;

    private String pcNameHi;

    private Integer pcNo;

    private String stateCode;

    private String stateName;

    private Long stateId;

    private String reservedCategory;

    private String wikidataQid;

    private Integer electionPhase2019;

    private LocalDate electionDate2019;

    private String currentMpName;

    private String currentMpParty;

    private Double areaSqKm;

    // Centroid coordinates for map marker
    private Double centroidLat;
    private Double centroidLng;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
