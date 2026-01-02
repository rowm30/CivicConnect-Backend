package com.civicconnect.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "state_boundaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateBoundary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state_id", unique = true, nullable = false)
    private String stateId;

    @Column(name = "state_name", nullable = false)
    private String stateName;

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
