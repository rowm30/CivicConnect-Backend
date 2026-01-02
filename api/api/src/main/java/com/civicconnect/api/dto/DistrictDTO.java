package com.civicconnect.api.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DistrictDTO {
    private Long id;
    private String districtId;
    private String districtName;
    private String stateName;
    private Integer stateLgd;
    private String distLgd;
    private String remarks;
    private Double areaSqKm;
    private Double centroidLat;
    private Double centroidLng;
}
