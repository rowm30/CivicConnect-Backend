package com.civicconnect.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for creating a new issue
 */
@Data
public class CreateIssueRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String imageUrl;

    @NotNull(message = "Category is required")
    private String category;

    private String priority;

    // Location
    private Double latitude;
    private Double longitude;
    private String locationName;
    private String districtName;
    private String stateName;

    // Constituency information (for location-based filtering)
    private String parliamentaryConstituency;
    private String assemblyConstituency;

    // Optional department assignment
    private String departmentName;
}
