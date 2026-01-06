package com.civicconnect.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Ward Councillor data transfer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WardCouncillorDTO {
    private Long id;
    private Integer wardNo;
    private String wardName;
    private String councillorName;
    private String partyAffiliation;
    private String city;
    private String state;
    private Integer electionYear;
    private String municipalityName;
    private String zone;
    private String phone;
    private String email;
    private String officeAddress;
    private String photoUrl;
    private String dataSource;
    private Integer sourceYear;
}
