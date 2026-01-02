package com.civicconnect.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "members_of_parliament", indexes = {
        @Index(name = "idx_mp_constituency", columnList = "constituency_name"),
        @Index(name = "idx_mp_state", columnList = "state_name"),
        @Index(name = "idx_mp_party", columnList = "party_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberOfParliament extends BaseEntity {

    @NotBlank(message = "Member name is required")
    @Size(max = 255)
    @Column(name = "member_name", nullable = false)
    private String memberName;

    @Size(max = 255)
    @Column(name = "party_name")
    private String partyName;

    @Size(max = 100)
    @Column(name = "party_abbreviation")
    private String partyAbbreviation;

    @Size(max = 255)
    @Column(name = "constituency_name")
    private String constituencyName;

    @Size(max = 100)
    @Column(name = "state_name")
    private String stateName;

    @Size(max = 50)
    @Column(name = "membership_status")
    private String membershipStatus;  // Sitting, Former, etc.

    @Size(max = 100)
    @Column(name = "lok_sabha_terms")
    private String lokSabhaTerms;  // e.g., "17,18" or "11,13,14,15,17,18"

    @Column(name = "current_term")
    private Integer currentTerm;  // 18 for current Lok Sabha

    @Size(max = 255)
    @Column(name = "photo_url")
    private String photoUrl;

    @Size(max = 100)
    @Column(name = "gender")
    private String gender;

    @Size(max = 100)
    @Column(name = "education")
    private String education;

    @Column(name = "age")
    private Integer age;

    @Size(max = 500)
    @Column(name = "email")
    private String email;

    @Size(max = 50)
    @Column(name = "phone")
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "constituency_id")
    private ParliamentaryConstituency constituency;
}
