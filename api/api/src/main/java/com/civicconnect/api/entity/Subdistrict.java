package com.civicconnect.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "subdistricts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subdistrict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subdistrict_id", unique = true, nullable = false)
    private String subdistrictId;

    @Column(name = "subdistrict_name", nullable = false)
    private String subdistrictName;

    @Column(name = "subdistrict_type")
    private String subdistrictType;

    @Column(name = "district_name")
    private String districtName;

    @Column(name = "state_name")
    private String stateName;

    @Column(name = "state_lgd")
    private String stateLgd;

    @Column(name = "dist_lgd")
    private String distLgd;

    @Column(name = "subdis_lgd")
    private String subdisLgd;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "boundary", columnDefinition = "geometry(MultiPolygon, 4326)")
    private Geometry boundary;

    @Column(name = "centroid", columnDefinition = "geometry(Point, 4326)")
    private Point centroid;

    @Column(name = "area_sq_km")
    private Double areaSqKm;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
