package com.civicconnect.api.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateCapitalDTO {
    private Long id;
    private String capitalId;
    private String stateName;
    private String capitalName;
    private Integer stateNo;
    private Double elevation;
    private Double latitude;
    private Double longitude;
}
