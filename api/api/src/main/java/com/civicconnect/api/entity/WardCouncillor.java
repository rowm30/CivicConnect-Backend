package com.civicconnect.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entity representing a Municipal Ward Councillor
 * Currently stores Delhi MCD ward councillors data
 * Can be extended for other cities/municipalities
 */
@Entity
@Table(name = "ward_councillors", indexes = {
        @Index(name = "idx_ward_no", columnList = "ward_no"),
        @Index(name = "idx_ward_name", columnList = "ward_name"),
        @Index(name = "idx_councillor_city", columnList = "city"),
        @Index(name = "idx_councillor_state", columnList = "state"),
        @Index(name = "idx_councillor_party", columnList = "party_affiliation")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WardCouncillor extends BaseEntity {

    /**
     * Ward number within the municipality
     */
    @Column(name = "ward_no", nullable = false)
    private Integer wardNo;

    /**
     * Name of the ward
     */
    @NotBlank(message = "Ward name is required")
    @Size(max = 255)
    @Column(name = "ward_name", nullable = false)
    private String wardName;

    /**
     * Full name of the councillor
     */
    @NotBlank(message = "Councillor name is required")
    @Size(max = 255)
    @Column(name = "councillor_name", nullable = false)
    private String councillorName;

    /**
     * Political party affiliation (e.g., AAP, BJP, INC, Independent)
     */
    @Size(max = 100)
    @Column(name = "party_affiliation")
    private String partyAffiliation;

    /**
     * City/Municipality name (e.g., Delhi, Mumbai)
     */
    @NotBlank(message = "City is required")
    @Size(max = 100)
    @Column(name = "city", nullable = false)
    private String city;

    /**
     * State name
     */
    @NotBlank(message = "State is required")
    @Size(max = 100)
    @Column(name = "state", nullable = false)
    private String state;

    /**
     * Year of election/data source
     */
    @Column(name = "election_year")
    private Integer electionYear;

    /**
     * Municipality/Corporation name (e.g., MCD, BMC, GHMC)
     */
    @Size(max = 100)
    @Column(name = "municipality_name")
    private String municipalityName;

    /**
     * Zone within the municipality (if applicable)
     */
    @Size(max = 100)
    @Column(name = "zone")
    private String zone;

    /**
     * Contact phone number
     */
    @Size(max = 50)
    @Column(name = "phone")
    private String phone;

    /**
     * Contact email
     */
    @Size(max = 255)
    @Column(name = "email")
    private String email;

    /**
     * Office address
     */
    @Size(max = 500)
    @Column(name = "office_address")
    private String officeAddress;

    /**
     * Photo URL
     */
    @Size(max = 500)
    @Column(name = "photo_url")
    private String photoUrl;

    /**
     * Data source (e.g., "MCD Website", "Election Commission")
     */
    @Size(max = 100)
    @Column(name = "data_source")
    private String dataSource;

    /**
     * Source year for the data
     */
    @Column(name = "source_year")
    private Integer sourceYear;
}
