package com.civicconnect.api.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MajorTownDTO {
    private Long id;
    private String townId;
    private String townName;
    private String districtName;
    private String stateName;
    private Double elevation;
    private Double latitude;
    private Double longitude;
}
