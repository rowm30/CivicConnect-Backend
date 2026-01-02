package com.civicconnect.api.dto;

import com.civicconnect.api.entity.MemberOfLegislativeAssembly;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for MLA data in API responses
 */
@Data
@Builder
public class MlaDTO {

    private Long id;
    private String memberName;
    private String memberNameLocal;
    private String gender;
    private Integer age;
    private String education;
    private String photoUrl;

    // Constituency
    private Integer acNo;
    private String constituencyName;
    private String reservedCategory;
    private String stateName;
    private String stateCode;
    private String districtName;

    // Party
    private String partyName;
    private String partyAbbreviation;

    // Election
    private Integer electionYear;
    private Integer votesReceived;
    private Double voteSharePercentage;
    private Integer winningMargin;
    private String runnerUpName;
    private String runnerUpParty;
    private Integer runnerUpVotes;

    // Criminal & Financial
    private Integer criminalCases;
    private String assetsDisplay;

    // Contact
    private String email;
    private String phone;

    // Status
    private String membershipStatus;

    // Data source
    private String dataSource;

    public static MlaDTO fromEntity(MemberOfLegislativeAssembly mla) {
        return MlaDTO.builder()
                .id(mla.getId())
                .memberName(mla.getMemberName())
                .memberNameLocal(mla.getMemberNameLocal())
                .gender(mla.getGender())
                .age(mla.getAge())
                .education(mla.getEducation())
                .photoUrl(mla.getPhotoUrl())
                .acNo(mla.getAcNo())
                .constituencyName(mla.getConstituencyName())
                .reservedCategory(mla.getReservedCategory())
                .stateName(mla.getStateName())
                .stateCode(mla.getStateCode())
                .districtName(mla.getDistrictName())
                .partyName(mla.getPartyName())
                .partyAbbreviation(mla.getPartyAbbreviation())
                .electionYear(mla.getElectionYear())
                .votesReceived(mla.getVotesReceived())
                .voteSharePercentage(mla.getVoteSharePercentage())
                .winningMargin(mla.getWinningMargin())
                .runnerUpName(mla.getRunnerUpName())
                .runnerUpParty(mla.getRunnerUpParty())
                .runnerUpVotes(mla.getRunnerUpVotes())
                .criminalCases(mla.getCriminalCases())
                .assetsDisplay(mla.getAssetsDisplay())
                .email(mla.getEmail())
                .phone(mla.getPhone())
                .membershipStatus(mla.getMembershipStatus())
                .dataSource(mla.getDataSource())
                .build();
    }
}
