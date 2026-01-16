package com.civicconnect.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing Prime Ministers of India
 * Part of the fixed GovMap hierarchy structure
 */
@Entity
@Table(name = "prime_ministers", indexes = {
        @Index(name = "idx_pm_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrimeMinister {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String nameHindi;

    private String gender;

    private Integer age;

    private String education;

    @Column(length = 1024)
    private String photoUrl;

    private String partyName;

    private String partyAbbreviation;

    // Parliamentary Constituency the PM represents
    private String constituencyName;

    private String constituencyState;

    private LocalDate termStartDate;

    private LocalDate termEndDate;

    private Integer termNumber; // 1st term, 2nd term, etc.

    @Column(length = 1024)
    private String email;

    @Column(length = 100)
    private String phone;

    @Column(length = 2048)
    private String pmoAddress;

    @Column(length = 1024)
    private String twitterHandle;

    @Column(length = 1024)
    private String facebookUrl;

    @Column(length = 1024)
    private String websiteUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private PMStatus status = PMStatus.CURRENT;

    @Column(columnDefinition = "TEXT")
    private String biography;

    // National level accountability metrics
    @Builder.Default
    private Long totalIssuesNationwide = 0L;

    @Builder.Default
    private Long resolvedIssuesNationwide = 0L;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum PMStatus {
        CURRENT,    // Currently serving
        FORMER,     // Previously served
        ACTING      // Acting PM (rare)
    }

    /**
     * Get resolution rate as percentage
     */
    public Double getResolutionRate() {
        if (totalIssuesNationwide == null || totalIssuesNationwide == 0) {
            return 0.0;
        }
        return (resolvedIssuesNationwide * 100.0) / totalIssuesNationwide;
    }
}
