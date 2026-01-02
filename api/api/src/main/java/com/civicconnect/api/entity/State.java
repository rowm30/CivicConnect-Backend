package com.civicconnect.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "states", indexes = {
        @Index(name = "idx_state_code", columnList = "code", unique = true),
        @Index(name = "idx_state_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class State extends BaseEntity {

    @NotBlank(message = "State name is required")
    @Size(max = 100)
    @Column(nullable = false)
    private String name;

    @Size(max = 100)
    @Column(name = "name_local")
    private String nameLocal;  // Name in local language (Hindi/regional)

    @NotBlank(message = "State code is required")
    @Size(max = 10)
    @Column(nullable = false, unique = true)
    private String code;  // e.g., "MH" for Maharashtra, "UP" for Uttar Pradesh

    @Column(name = "iso_code")
    @Size(max = 10)
    private String isoCode;  // ISO 3166-2:IN code e.g., "IN-MH"

    @Enumerated(EnumType.STRING)
    @Column(name = "state_type", nullable = false)
    private StateType stateType;

    @Column(name = "capital")
    @Size(max = 100)
    private String capital;

    @Column(name = "largest_city")
    @Size(max = 100)
    private String largestCity;

    @Column(name = "official_languages")
    private String officialLanguages;  // Comma-separated

    @Column(name = "total_districts")
    private Integer totalDistricts;

    @Column(name = "total_lok_sabha_seats")
    private Integer totalLokSabhaSeats;

    @Column(name = "total_vidhan_sabha_seats")
    private Integer totalVidhanSabhaSeats;

    @Column(name = "has_legislative_council")
    private Boolean hasLegislativeCouncil = false;  // Vidhan Parishad

    @Column(name = "total_vidhan_parishad_seats")
    private Integer totalVidhanParishadSeats;

    @Column(name = "area_sq_km")
    private Double areaSqKm;

    @Column(name = "population")
    private Long population;  // As per latest census

    @Column(name = "census_year")
    private Integer censusYear;

    @Column(name = "official_website")
    private String officialWebsite;

    @Column(name = "cm_grievance_portal")
    private String cmGrievancePortal;

    // Geometry field will be added later with PostGIS
    // @Column(columnDefinition = "geometry(MultiPolygon, 4326)")
    // private Geometry boundary;

    public enum StateType {
        STATE,
        UNION_TERRITORY,
        UNION_TERRITORY_WITH_LEGISLATURE,
        NATIONAL_CAPITAL_TERRITORY
    }
}