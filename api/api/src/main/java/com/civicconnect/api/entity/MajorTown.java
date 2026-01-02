package com.civicconnect.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "major_towns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MajorTown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "town_id", unique = true, nullable = false)
    private String townId;

    @Column(name = "town_name", nullable = false)
    private String townName;

    @Column(name = "district_name")
    private String districtName;

    @Column(name = "state_name")
    private String stateName;

    @Column(name = "elevation")
    private Double elevation;

    @Column(name = "location", columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
