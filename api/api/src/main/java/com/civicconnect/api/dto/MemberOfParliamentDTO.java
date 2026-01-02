package com.civicconnect.api.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberOfParliamentDTO {

    private Long id;

    private String memberName;

    private String partyName;

    private String partyAbbreviation;

    private String constituencyName;

    private String stateName;

    private String membershipStatus;

    private String lokSabhaTerms;

    private Integer currentTerm;

    private String photoUrl;

    private String gender;

    private String education;

    private Integer age;

    private String email;

    private String phone;

    private Long constituencyId;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
