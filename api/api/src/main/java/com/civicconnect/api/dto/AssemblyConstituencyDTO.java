package com.civicconnect.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssemblyConstituencyDTO {
    private Long id;
    private String acId;
    private String acName;
    private Integer acNo;
    private String stateCode;
    private String stateName;
    private String districtCode;
    private String districtName;
    private Integer pcNo;
    private String pcName;
    private String pcId;
    private String reservedCategory;
    private String status;
    private String currentMlaName;
    private String currentMlaParty;
    private Double areaSqKm;
    private Double centroidLat;
    private Double centroidLng;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
