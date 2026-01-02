package com.civicconnect.api.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DistrictHeadquarterDTO {
    private Long id;
    private String hqId;
    private String hqName;
    private String townName;
    private String districtName;
    private String stateName;
    private String talukName;
    private Double latitude;
    private Double longitude;
}
