package com.civicconnect.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "state_capitals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateCapital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "capital_id", unique = true, nullable = false)
    private String capitalId;

    @Column(name = "state_name", nullable = false)
    private String stateName;

    @Column(name = "capital_name", nullable = false)
    private String capitalName;

    @Column(name = "state_no")
    private Integer stateNo;

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
