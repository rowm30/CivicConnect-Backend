package com.civicconnect.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "districts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class District {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "district_id", unique = true, nullable = false)
    private String districtId;

    @Column(name = "district_name", nullable = false)
    private String districtName;

    @Column(name = "state_name", nullable = false)
    private String stateName;

    @Column(name = "state_lgd")
    private Integer stateLgd;

    @Column(name = "dist_lgd")
    private String distLgd;

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

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
