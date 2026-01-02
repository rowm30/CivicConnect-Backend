package com.civicconnect.api.dto;

import lombok.Data;

/**
 * Request DTO for AI-powered issue analysis
 * Contains URLs to the image and optional audio for analysis
 */
@Data
public class IssueAnalysisRequest {

    /**
     * URL to the issue image (required)
     * Can be a full URL or a relative path like /uploads/issues/images/...
     */
    private String imageUrl;

    /**
     * URL to the voice recording (optional)
     * If provided, will be transcribed and used to enhance analysis
     */
    private String audioUrl;

    /**
     * User's current location for context
     */
    private Double latitude;
    private Double longitude;
    private String locationName;
}
