package com.civicconnect.api.dto;

import com.civicconnect.api.entity.RepresentativeRating.RepresentativeType;
import lombok.*;

import java.util.List;

/**
 * DTOs for GovMap Fixed Hierarchy Structure
 *
 * India's Democratic Hierarchy:
 *
 * NATIONAL Level:
 *   - Prime Minister (Head of Government)
 *   - Member of Parliament (MP) - Lok Sabha, directly elected from Parliamentary Constituency
 *
 * STATE Level:
 *   - Chief Minister (Head of State Government)
 *   - Member of Legislative Assembly (MLA) - directly elected from Assembly Constituency
 *
 * LOCAL Level (Urban):
 *   - Mayor / Municipal Commissioner (Corporation Head)
 *   - Ward Councillor - directly elected from Municipal Ward
 *
 * The hierarchy is FIXED - all nodes are always shown regardless of data availability.
 * If data is not available for a node, it shows "Data Not Available" with appropriate UI.
 */
public class GovMapDTO {

    /**
     * Represents a single node in the government hierarchy.
     * Always contains structural information, with optional data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HierarchyNode {
        // Structural fields (always present)
        private String nodeType;        // "PRIME_MINISTER", "MP", "CHIEF_MINISTER", "MLA", "MAYOR", "WARD_COUNCILLOR", "CITIZEN"
        private String designation;     // Human-readable designation
        private String designationHindi; // Designation in Hindi
        private Integer level;          // 0 = citizen (bottom), higher = more authority
        private Boolean isDataAvailable; // Whether actual data exists for this node

        // Data fields (only present if isDataAvailable = true)
        private Long id;
        private String name;
        private String party;
        private String partyAbbreviation;
        private String photoUrl;
        private String constituency;    // PC for MP, AC for MLA, Ward for Councillor
        private String state;
        private String city;            // For Mayor/Councillor

        // Accountability metrics (only if data available)
        private Double approvalRating;  // 0-5 stars
        private Long totalRatings;
        private String approvalLevel;   // "Excellent", "Good", "Average", "Poor", "Very Poor", "Not Rated"
        private Integer accountabilityScore; // 0-100
        private Long issuesRegistered;
        private Long issuesResolved;
        private Double resolutionRate;  // Percentage

        // User interaction (only if data available)
        private Integer userRating;     // User's own rating (1-5), null if not rated
        private Boolean canRate;        // Whether user can rate this representative

        // Contact info (only if data available)
        private String email;
        private String phone;
        private String address;
        private String twitterHandle;

        // Placeholder message when data not available
        private String unavailableMessage; // e.g., "Ward councillor data not available for this area"
    }

    /**
     * Fixed hierarchy structure for a specific mode (LOCAL/STATE/NATIONAL)
     * Always returns exactly the expected nodes for each mode.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FixedHierarchyResponse {
        private String mode;            // "LOCAL", "STATE", "NATIONAL"
        private String modeName;        // "Local Government", "State Government", "National Government"
        private String modeDescription; // Description of this level of government

        // Location context
        private String stateName;
        private String stateCode;
        private String districtName;
        private String cityName;
        private String wardName;
        private Integer wardNumber;
        private String assemblyConstituency;
        private String parliamentaryConstituency;

        // Fixed hierarchy nodes (always present, in order from citizen to top)
        private List<HierarchyNode> nodes;

        // Citizen node (always first)
        private CitizenInfo citizen;

        // Summary statistics
        private HierarchySummary summary;
    }

    /**
     * Citizen information for the hierarchy
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CitizenInfo {
        private Long userId;
        private String name;
        private String location;        // Formatted location string
        private Long issuesReported;
        private Long issuesResolved;
        private Long ratingsGiven;
        private Integer voicePower;     // 0-100, based on civic participation
        private String voicePowerMessage;
    }

    /**
     * Summary statistics for the hierarchy
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HierarchySummary {
        private Integer totalNodesInHierarchy;
        private Integer nodesWithData;
        private Integer nodesWithoutData;
        private Long totalIssuesInArea;
        private Long resolvedIssuesInArea;
        private Double overallResolutionRate;
        private Double averageAccountabilityScore;
    }

    /**
     * Request to get fixed hierarchy for a location
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HierarchyRequest {
        private String mode;            // "LOCAL", "STATE", "NATIONAL"
        private Double latitude;
        private Double longitude;
        private String stateCode;       // Alternative to lat/lng
        private String stateName;
        private String cityName;
        private String assemblyConstituencyId;
        private String parliamentaryConstituencyId;
    }

    /**
     * Complete Gov Map response with all three hierarchies
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompleteGovMapResponse {
        private CitizenInfo citizen;
        private FixedHierarchyResponse localHierarchy;
        private FixedHierarchyResponse stateHierarchy;
        private FixedHierarchyResponse nationalHierarchy;

        // Location metadata
        private String formattedLocation;
        private Boolean isLocationAvailable;
        private String locationMessage;  // Message if location not available
    }

    /**
     * Structure definition for each hierarchy mode
     * Used by both frontend and backend to ensure consistency
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HierarchyStructure {
        private String mode;
        private List<NodeDefinition> nodeDefinitions;
    }

    /**
     * Definition of a node in the hierarchy (structural, not data)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeDefinition {
        private String nodeType;
        private String designation;
        private String designationHindi;
        private Integer level;
        private String description;
        private RepresentativeType representativeType; // For rating purposes
    }

    /**
     * Static hierarchy structure definitions for India's democratic system
     */
    public static class HierarchyDefinitions {

        public static final NodeDefinition CITIZEN = NodeDefinition.builder()
                .nodeType("CITIZEN")
                .designation("Citizen")
                .designationHindi("नागरिक")
                .level(0)
                .description("You - the source of democratic power")
                .build();

        // LOCAL hierarchy
        public static final NodeDefinition WARD_COUNCILLOR = NodeDefinition.builder()
                .nodeType("WARD_COUNCILLOR")
                .designation("Ward Councillor")
                .designationHindi("पार्षद")
                .level(1)
                .description("Directly elected representative for your municipal ward")
                .representativeType(RepresentativeType.WARD_COUNCILLOR)
                .build();

        public static final NodeDefinition MAYOR = NodeDefinition.builder()
                .nodeType("MAYOR")
                .designation("Mayor")
                .designationHindi("महापौर")
                .level(2)
                .description("Head of the Municipal Corporation")
                .representativeType(RepresentativeType.MAYOR)
                .build();

        // STATE hierarchy
        public static final NodeDefinition MLA = NodeDefinition.builder()
                .nodeType("MLA")
                .designation("Member of Legislative Assembly")
                .designationHindi("विधायक (MLA)")
                .level(1)
                .description("Directly elected representative for your Assembly Constituency")
                .representativeType(RepresentativeType.MLA)
                .build();

        public static final NodeDefinition CHIEF_MINISTER = NodeDefinition.builder()
                .nodeType("CHIEF_MINISTER")
                .designation("Chief Minister")
                .designationHindi("मुख्यमंत्री")
                .level(2)
                .description("Head of the State Government")
                .representativeType(RepresentativeType.CHIEF_MINISTER)
                .build();

        // NATIONAL hierarchy
        public static final NodeDefinition MP = NodeDefinition.builder()
                .nodeType("MP")
                .designation("Member of Parliament")
                .designationHindi("सांसद (MP)")
                .level(1)
                .description("Directly elected representative for your Parliamentary Constituency")
                .representativeType(RepresentativeType.MP)
                .build();

        public static final NodeDefinition PRIME_MINISTER = NodeDefinition.builder()
                .nodeType("PRIME_MINISTER")
                .designation("Prime Minister")
                .designationHindi("प्रधानमंत्री")
                .level(2)
                .description("Head of the Government of India")
                .representativeType(RepresentativeType.PRIME_MINISTER)
                .build();

        /**
         * Get structure for LOCAL mode
         * Citizen → Ward Councillor → Mayor
         */
        public static HierarchyStructure getLocalStructure() {
            return HierarchyStructure.builder()
                    .mode("LOCAL")
                    .nodeDefinitions(List.of(CITIZEN, WARD_COUNCILLOR, MAYOR))
                    .build();
        }

        /**
         * Get structure for STATE mode
         * Citizen → MLA → Chief Minister
         */
        public static HierarchyStructure getStateStructure() {
            return HierarchyStructure.builder()
                    .mode("STATE")
                    .nodeDefinitions(List.of(CITIZEN, MLA, CHIEF_MINISTER))
                    .build();
        }

        /**
         * Get structure for NATIONAL mode
         * Citizen → MP → Prime Minister
         */
        public static HierarchyStructure getNationalStructure() {
            return HierarchyStructure.builder()
                    .mode("NATIONAL")
                    .nodeDefinitions(List.of(CITIZEN, MP, PRIME_MINISTER))
                    .build();
        }
    }

    /**
     * Request to submit information about a government node
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeInfoSubmissionRequest {
        // Node identification
        private String nodeType;        // WARD_COUNCILLOR, MLA, MP, MAYOR, CHIEF_MINISTER
        private String hierarchyMode;   // LOCAL, STATE, NATIONAL

        // Location context
        private String stateName;
        private String stateCode;
        private String cityName;
        private String districtName;
        private String wardName;
        private Integer wardNumber;
        private String assemblyConstituency;
        private String parliamentaryConstituency;

        // Official information (required)
        private String officialName;
        private String party;
        private String partyAbbreviation;
        private String designation;
        private String photoUrl;

        // Contact information (optional)
        private String email;
        private String phone;
        private String address;
        private String twitterHandle;

        // Additional info
        private String additionalInfo;
        private String sourceUrl;
        private String sourceDescription;
    }

    /**
     * Response for node info submission
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeInfoSubmissionResponse {
        private Long submissionId;
        private String status;
        private String message;
        private String nodeType;
        private String officialName;
        private String submittedAt;
    }

    /**
     * Response for listing user's submissions
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSubmissionsResponse {
        private Long totalSubmissions;
        private Long pendingCount;
        private Long approvedCount;
        private Long rejectedCount;
        private List<SubmissionSummary> submissions;
    }

    /**
     * Summary of a single submission
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmissionSummary {
        private Long id;
        private String nodeType;
        private String hierarchyMode;
        private String officialName;
        private String stateName;
        private String status;
        private String submittedAt;
        private String reviewNotes;
    }
}
