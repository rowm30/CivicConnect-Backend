package com.civicconnect.api.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateBoundaryDTO {
    private Long id;
    private String stateId;
    private String stateName;
    private Double areaSqKm;
    private Double centroidLat;
    private Double centroidLng;
}
