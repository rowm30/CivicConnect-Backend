package com.civicconnect.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Benefit entity for government schemes and benefits
 * Supports eligibility checking for Benefit Check feature
 */
@Entity
@Table(name = "benefits", indexes = {
    @Index(name = "idx_benefit_category", columnList = "category"),
    @Index(name = "idx_benefit_level", columnList = "government_level"),
    @Index(name = "idx_benefit_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
public class Benefit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description")
    private String shortDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BenefitCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "government_level", nullable = false)
    private GovernmentLevel governmentLevel;

    // Financial details
    @Column(name = "benefit_amount")
    private BigDecimal benefitAmount;

    @Column(name = "benefit_type")
    private String benefitType; // "CASH", "SUBSIDY", "SERVICE", "GOODS"

    // Eligibility criteria
    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "min_income")
    private BigDecimal minIncome;

    @Column(name = "max_income")
    private BigDecimal maxIncome;

    @Column(name = "gender_requirement")
    private String genderRequirement; // "MALE", "FEMALE", "OTHER", "ALL"

    @Column(name = "occupation_requirement")
    private String occupationRequirement;

    @Column(name = "education_requirement")
    private String educationRequirement;

    @Column(name = "caste_category")
    private String casteCategory; // "SC", "ST", "OBC", "GENERAL", "ALL"

    @Column(name = "state_specific")
    private String stateSpecific; // State name if state-specific

    @Column(name = "district_specific")
    private String districtSpecific;

    // Application details
    @Column(name = "application_url")
    private String applicationUrl;

    @Column(name = "documents_required", columnDefinition = "TEXT")
    private String documentsRequired;

    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @Column(name = "scheme_start_date")
    private LocalDate schemeStartDate;

    @Column(name = "scheme_end_date")
    private LocalDate schemeEndDate;

    // Department info
    @Column(name = "department_name")
    private String departmentName;

    @Column(name = "contact_info")
    private String contactInfo;

    @Column(name = "helpline_number")
    private String helplineNumber;

    // Stats
    @Column(name = "total_beneficiaries")
    private Long totalBeneficiaries;

    @Column(name = "total_amount_disbursed")
    private BigDecimal totalAmountDisbursed;

    // Status
    @Column(name = "is_active")
    private Boolean isActive = true;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Eligibility rules as JSON (for complex rules)
    @Column(name = "eligibility_rules", columnDefinition = "TEXT")
    private String eligibilityRulesJson;

    public enum BenefitCategory {
        EDUCATION,
        HEALTH,
        HOUSING,
        AGRICULTURE,
        EMPLOYMENT,
        SOCIAL_SECURITY,
        WOMEN_CHILD,
        DISABILITY,
        SENIOR_CITIZEN,
        MINORITY,
        SKILL_DEVELOPMENT,
        FINANCIAL_INCLUSION,
        OTHER
    }

    public enum GovernmentLevel {
        CENTRAL,
        STATE,
        DISTRICT,
        LOCAL
    }
}
