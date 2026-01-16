package com.civicconnect.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing Mayors/Municipal Commissioners of Indian Cities
 * Part of the fixed GovMap hierarchy structure
 */
@Entity
@Table(name = "mayors", indexes = {
        @Index(name = "idx_mayor_state_code", columnList = "stateCode"),
        @Index(name = "idx_mayor_city", columnList = "cityName"),
        @Index(name = "idx_mayor_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mayor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String stateCode;

    @Column(nullable = false)
    private String stateName;

    @Column(nullable = false)
    private String cityName;

    @Column(nullable = false)
    private String municipalBodyName; // e.g., "Municipal Corporation of Delhi", "BMC"

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private MunicipalBodyType municipalBodyType = MunicipalBodyType.MUNICIPAL_CORPORATION;

    @Column(nullable = false)
    private String name;

    private String nameLocal;

    private String gender;

    private Integer age;

    private String education;

    @Column(length = 1024)
    private String photoUrl;

    private String partyName;

    private String partyAbbreviation;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private MayorType mayorType = MayorType.ELECTED_MAYOR;

    private LocalDate termStartDate;

    private LocalDate termEndDate;

    private Integer termNumber;

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
    private MayorStatus status = MayorStatus.CURRENT;

    @Column(columnDefinition = "TEXT")
    private String biography;

    // Total wards in the municipal body
    private Integer totalWards;

    // Accountability metrics
    @Builder.Default
    private Long totalIssuesInCity = 0L;

    @Builder.Default
    private Long resolvedIssuesInCity = 0L;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum MunicipalBodyType {
        MUNICIPAL_CORPORATION,      // Nagar Nigam
        MUNICIPAL_COUNCIL,          // Nagar Parishad
        NAGAR_PANCHAYAT,           // Town Panchayat
        CANTONMENT_BOARD,          // Military cantonment
        NOTIFIED_AREA_COMMITTEE,   // NAC
        TOWN_AREA_COMMITTEE        // TAC
    }

    public enum MayorType {
        ELECTED_MAYOR,              // Directly elected by citizens
        COUNCIL_ELECTED_MAYOR,      // Elected by councillors
        MUNICIPAL_COMMISSIONER,     // IAS officer appointed
        MUNICIPAL_CHAIRPERSON       // For smaller municipalities
    }

    public enum MayorStatus {
        CURRENT,
        FORMER,
        ACTING
    }

    /**
     * Get resolution rate as percentage
     */
    public Double getResolutionRate() {
        if (totalIssuesInCity == null || totalIssuesInCity == 0) {
            return 0.0;
        }
        return (resolvedIssuesInCity * 100.0) / totalIssuesInCity;
    }
}
