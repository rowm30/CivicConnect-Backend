package com.civicconnect.api.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubdistrictDTO {
    private Long id;
    private String subdistrictId;
    private String subdistrictName;
    private String subdistrictType;
    private String districtName;
    private String stateName;
    private String stateLgd;
    private String distLgd;
    private String subdisLgd;
    private String remarks;
    private Double areaSqKm;
    private Double centroidLat;
    private Double centroidLng;
}
