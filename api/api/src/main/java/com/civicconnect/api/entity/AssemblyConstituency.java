package com.civicconnect.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "assembly_constituencies", indexes = {
        @Index(name = "idx_ac_state_code", columnList = "state_code"),
        @Index(name = "idx_ac_state_name", columnList = "state_name"),
        @Index(name = "idx_ac_district_name", columnList = "district_name"),
        @Index(name = "idx_ac_pc_id", columnList = "pc_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssemblyConstituency extends BaseEntity {

    @Column(name = "ac_id")
    @Size(max = 20)
    private String acId;

    @NotBlank(message = "Constituency name is required")
    @Size(max = 200)
    @Column(name = "ac_name", nullable = false)
    private String acName;

    @Column(name = "ac_no")
    private Integer acNo;

    @Size(max = 5)
    @Column(name = "state_code")
    private String stateCode;

    @Size(max = 100)
    @Column(name = "state_name")
    private String stateName;

    @Size(max = 10)
    @Column(name = "district_code")
    private String districtCode;

    @Size(max = 200)
    @Column(name = "district_name")
    private String districtName;

    @Column(name = "pc_no")
    private Integer pcNo;

    @Size(max = 200)
    @Column(name = "pc_name")
    private String pcName;

    @Size(max = 10)
    @Column(name = "pc_id")
    private String pcId;

    @Size(max = 20)
    @Column(name = "reserved_category")
    private String reservedCategory;  // GEN, SC, ST

    @Size(max = 50)
    @Column(name = "status")
    private String status;  // Pre delimitation, Post delimitation

    @Size(max = 200)
    @Column(name = "current_mla_name")
    private String currentMlaName;

    @Size(max = 200)
    @Column(name = "current_mla_party")
    private String currentMlaParty;

    @Column(name = "boundary", columnDefinition = "geometry(MultiPolygon, 4326)")
    private Geometry boundary;

    @Column(name = "centroid", columnDefinition = "geometry(Point, 4326)")
    private Point centroid;

    @Column(name = "area_sq_km")
    private Double areaSqKm;
}
