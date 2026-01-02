package com.civicconnect.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing a Member of Legislative Assembly (MLA)
 * Stores detailed information about elected MLAs including election data
 */
@Entity
@Table(name = "members_of_legislative_assembly", indexes = {
        @Index(name = "idx_mla_constituency", columnList = "constituency_name"),
        @Index(name = "idx_mla_state", columnList = "state_name"),
        @Index(name = "idx_mla_party", columnList = "party_name"),
        @Index(name = "idx_mla_ac_no", columnList = "ac_no"),
        @Index(name = "idx_mla_election_year", columnList = "election_year")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberOfLegislativeAssembly extends BaseEntity {

    // Basic Information
    @NotBlank(message = "Member name is required")
    @Size(max = 255)
    @Column(name = "member_name", nullable = false)
    private String memberName;

    @Size(max = 255)
    @Column(name = "member_name_local")
    private String memberNameLocal;  // Name in regional language

    @Size(max = 20)
    @Column(name = "gender")
    private String gender;

    @Column(name = "age")
    private Integer age;

    @Size(max = 255)
    @Column(name = "education")
    private String education;

    @Size(max = 500)
    @Column(name = "photo_url")
    private String photoUrl;

    // Constituency Information
    @Column(name = "ac_no")
    private Integer acNo;

    @NotBlank(message = "Constituency name is required")
    @Size(max = 255)
    @Column(name = "constituency_name", nullable = false)
    private String constituencyName;

    @Size(max = 20)
    @Column(name = "reserved_category")
    private String reservedCategory;  // GEN, SC, ST

    @Size(max = 100)
    @Column(name = "state_name")
    private String stateName;

    @Size(max = 10)
    @Column(name = "state_code")
    private String stateCode;

    @Size(max = 200)
    @Column(name = "district_name")
    private String districtName;

    // Party Information
    @Size(max = 255)
    @Column(name = "party_name")
    private String partyName;

    @Size(max = 50)
    @Column(name = "party_abbreviation")
    private String partyAbbreviation;

    // Election Information
    @Column(name = "election_year")
    private Integer electionYear;

    @Column(name = "election_date")
    private LocalDate electionDate;

    @Column(name = "votes_received")
    private Integer votesReceived;

    @Column(name = "vote_share_percentage")
    private Double voteSharePercentage;

    @Column(name = "winning_margin")
    private Integer winningMargin;

    @Size(max = 255)
    @Column(name = "runner_up_name")
    private String runnerUpName;

    @Size(max = 100)
    @Column(name = "runner_up_party")
    private String runnerUpParty;

    @Column(name = "runner_up_votes")
    private Integer runnerUpVotes;

    @Column(name = "total_electors")
    private Integer totalElectors;

    @Column(name = "total_votes_polled")
    private Integer totalVotesPolled;

    @Column(name = "voter_turnout_percentage")
    private Double voterTurnoutPercentage;

    // Criminal & Financial Information (from ADR/MyNeta)
    @Column(name = "criminal_cases")
    private Integer criminalCases;

    @Column(name = "serious_criminal_cases")
    private Integer seriousCriminalCases;

    @Column(name = "total_assets")
    private BigDecimal totalAssets;

    @Column(name = "total_liabilities")
    private BigDecimal totalLiabilities;

    @Size(max = 100)
    @Column(name = "assets_display")
    private String assetsDisplay;  // "5 Crore+" human readable

    // Contact Information
    @Size(max = 500)
    @Column(name = "email")
    private String email;

    @Size(max = 100)
    @Column(name = "phone")
    private String phone;

    @Size(max = 500)
    @Column(name = "address")
    private String address;

    // Membership Status
    @Size(max = 50)
    @Column(name = "membership_status")
    private String membershipStatus;  // Sitting, Former, Resigned, Disqualified

    @Column(name = "term_start_date")
    private LocalDate termStartDate;

    @Column(name = "term_end_date")
    private LocalDate termEndDate;

    @Column(name = "term_number")
    private Integer termNumber;  // Which term (1st, 2nd, etc.)

    // Data Source Tracking
    @Size(max = 100)
    @Column(name = "data_source")
    private String dataSource;  // MyNeta, ECI, Bihar Vidhan Sabha, etc.

    @Size(max = 500)
    @Column(name = "source_url")
    private String sourceUrl;

    @Size(max = 100)
    @Column(name = "source_candidate_id")
    private String sourceCandidateId;  // ID from source website

    // Relationship to Assembly Constituency
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assembly_constituency_id")
    private AssemblyConstituency assemblyConstituency;
}
