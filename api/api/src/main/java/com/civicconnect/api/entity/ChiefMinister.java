package com.civicconnect.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing Chief Ministers of Indian States/UTs
 * Part of the fixed GovMap hierarchy structure
 */
@Entity
@Table(name = "chief_ministers", indexes = {
        @Index(name = "idx_cm_state_code", columnList = "stateCode"),
        @Index(name = "idx_cm_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChiefMinister {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String stateCode;

    @Column(nullable = false)
    private String stateName;

    @Column(nullable = false)
    private String name;

    private String nameLocal; // Name in local language

    private String gender;

    private Integer age;

    private String education;

    @Column(length = 1024)
    private String photoUrl;

    private String partyName;

    private String partyAbbreviation;

    private LocalDate termStartDate;

    private LocalDate termEndDate;

    private Integer termNumber; // 1st term, 2nd term, etc.

    @Column(length = 1024)
    private String email;

    @Column(length = 100)
    private String phone;

    @Column(length = 2048)
    private String address;

    @Column(length = 2048)
    private String officeAddress;

    @Column(length = 1024)
    private String twitterHandle;

    @Column(length = 1024)
    private String facebookUrl;

    @Column(length = 1024)
    private String websiteUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private CMStatus status = CMStatus.CURRENT;

    @Column(columnDefinition = "TEXT")
    private String biography;

    // Accountability metrics (can be updated periodically)
    @Builder.Default
    private Long totalIssuesInState = 0L;

    @Builder.Default
    private Long resolvedIssuesInState = 0L;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum CMStatus {
        CURRENT,    // Currently serving
        FORMER,     // Previously served
        ACTING      // Acting CM
    }

    /**
     * Get resolution rate as percentage
     */
    public Double getResolutionRate() {
        if (totalIssuesInState == null || totalIssuesInState == 0) {
            return 0.0;
        }
        return (resolvedIssuesInState * 100.0) / totalIssuesInState;
    }
}
