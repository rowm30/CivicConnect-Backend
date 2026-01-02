package com.civicconnect.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import java.time.LocalDate;

@Entity
@Table(name = "parliamentary_constituencies", indexes = {
        @Index(name = "idx_pc_state_code", columnList = "state_code"),
        @Index(name = "idx_pc_pc_name", columnList = "pc_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParliamentaryConstituency extends BaseEntity {

    @Column(name = "pc_id", unique = true)
    @Size(max = 10)
    private String pcId;

    @NotBlank(message = "Constituency name is required")
    @Size(max = 255)
    @Column(name = "pc_name", nullable = false)
    private String pcName;

    @Size(max = 255)
    @Column(name = "pc_name_hi")
    private String pcNameHi;  // Name in Hindi

    @Column(name = "pc_no")
    private Integer pcNo;

    @Size(max = 10)
    @Column(name = "state_code")
    private String stateCode;

    @Size(max = 100)
    @Column(name = "state_name")
    private String stateName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_id")
    private State state;

    @Size(max = 50)
    @Column(name = "reserved_category")
    private String reservedCategory;  // GEN, SC, ST

    @Size(max = 20)
    @Column(name = "wikidata_qid")
    private String wikidataQid;

    @Column(name = "election_phase_2019")
    private Integer electionPhase2019;

    @Column(name = "election_date_2019")
    private LocalDate electionDate2019;

    @Size(max = 255)
    @Column(name = "current_mp_name")
    private String currentMpName;

    @Size(max = 100)
    @Column(name = "current_mp_party")
    private String currentMpParty;

    @Column(name = "boundary", columnDefinition = "geometry(MultiPolygon, 4326)")
    private Geometry boundary;

    @Column(name = "centroid", columnDefinition = "geometry(Point, 4326)")
    private Point centroid;

    @Column(name = "area_sq_km")
    private Double areaSqKm;
}
