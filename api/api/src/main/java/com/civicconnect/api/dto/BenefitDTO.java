package com.civicconnect.api.dto;

import com.civicconnect.api.entity.Benefit;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Benefit DTO for API responses
 */
@Data
@Builder
public class BenefitDTO {
    private Long id;
    private String name;
    private String description;
    private String shortDescription;
    private String category;
    private String governmentLevel;

    // Financial
    private BigDecimal benefitAmount;
    private String benefitType;

    // Eligibility summary
    private String eligibilitySummary;
    private Integer minAge;
    private Integer maxAge;
    private BigDecimal maxIncome;
    private String genderRequirement;
    private String casteCategory;
    private String stateSpecific;

    // Application
    private String applicationUrl;
    private String documentsRequired;
    private LocalDate applicationDeadline;

    // Department
    private String departmentName;
    private String helplineNumber;

    // Stats
    private Long totalBeneficiaries;

    // User-specific
    private Boolean isSaved;
    private Boolean isApplied;
    private String applicationStatus;

    // Match score (0-100) based on eligibility
    private Integer matchScore;

    private LocalDateTime createdAt;

    public static BenefitDTO fromEntity(Benefit benefit) {
        return fromEntity(benefit, null, null, null);
    }

    public static BenefitDTO fromEntity(Benefit benefit, Boolean isSaved, Boolean isApplied, String applicationStatus) {
        String eligibilitySummary = buildEligibilitySummary(benefit);

        return BenefitDTO.builder()
                .id(benefit.getId())
                .name(benefit.getName())
                .description(benefit.getDescription())
                .shortDescription(benefit.getShortDescription())
                .category(benefit.getCategory().name())
                .governmentLevel(benefit.getGovernmentLevel().name())
                .benefitAmount(benefit.getBenefitAmount())
                .benefitType(benefit.getBenefitType())
                .eligibilitySummary(eligibilitySummary)
                .minAge(benefit.getMinAge())
                .maxAge(benefit.getMaxAge())
                .maxIncome(benefit.getMaxIncome())
                .genderRequirement(benefit.getGenderRequirement())
                .casteCategory(benefit.getCasteCategory())
                .stateSpecific(benefit.getStateSpecific())
                .applicationUrl(benefit.getApplicationUrl())
                .documentsRequired(benefit.getDocumentsRequired())
                .applicationDeadline(benefit.getApplicationDeadline())
                .departmentName(benefit.getDepartmentName())
                .helplineNumber(benefit.getHelplineNumber())
                .totalBeneficiaries(benefit.getTotalBeneficiaries())
                .isSaved(isSaved)
                .isApplied(isApplied)
                .applicationStatus(applicationStatus)
                .createdAt(benefit.getCreatedAt())
                .build();
    }

    private static String buildEligibilitySummary(Benefit b) {
        StringBuilder sb = new StringBuilder();
        if (b.getMinAge() != null || b.getMaxAge() != null) {
            if (b.getMinAge() != null && b.getMaxAge() != null) {
                sb.append("Age ").append(b.getMinAge()).append("-").append(b.getMaxAge()).append(" years");
            } else if (b.getMinAge() != null) {
                sb.append("Age ").append(b.getMinAge()).append("+ years");
            } else {
                sb.append("Below ").append(b.getMaxAge()).append(" years");
            }
            sb.append(" • ");
        }
        if (b.getMaxIncome() != null) {
            sb.append("Income below ₹").append(b.getMaxIncome().divide(BigDecimal.valueOf(100000)))
              .append(" LPA • ");
        }
        if (b.getGenderRequirement() != null && !b.getGenderRequirement().equals("ALL")) {
            sb.append(b.getGenderRequirement()).append(" only • ");
        }
        if (b.getCasteCategory() != null && !b.getCasteCategory().equals("ALL")) {
            sb.append(b.getCasteCategory()).append(" category • ");
        }

        String result = sb.toString();
        if (result.endsWith(" • ")) {
            result = result.substring(0, result.length() - 3);
        }
        return result.isEmpty() ? "Open to all eligible citizens" : result;
    }
}
