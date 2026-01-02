package com.civicconnect.api.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO containing AI-generated suggestions for issue form auto-fill
 */
@Data
@Builder
public class IssueAnalysisResponse {

    /**
     * Suggested title for the issue (max 100 chars)
     */
    private String suggestedTitle;

    /**
     * Suggested description with details about the issue
     */
    private String suggestedDescription;

    /**
     * Suggested category from the available categories:
     * ROADS, WATER, ELECTRICITY, WASTE, SAFETY, PARKS, BUILDING, TRAFFIC, NOISE, OTHER
     */
    private String suggestedCategory;

    /**
     * Suggested priority: LOW, MEDIUM, HIGH, URGENT
     */
    private String suggestedPriority;

    /**
     * Transcription of the voice recording (if provided)
     */
    private String audioTranscription;

    /**
     * Confidence score for the analysis (0.0 to 1.0)
     */
    private Float confidence;

    /**
     * Whether the analysis was successful
     */
    private boolean success;

    /**
     * Error message if analysis failed
     */
    private String errorMessage;

    /**
     * Create a failure response
     */
    public static IssueAnalysisResponse failure(String errorMessage) {
        return IssueAnalysisResponse.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Create a success response with suggestions
     */
    public static IssueAnalysisResponse success(
            String title,
            String description,
            String category,
            String priority,
            String transcription,
            Float confidence) {
        return IssueAnalysisResponse.builder()
                .suggestedTitle(title)
                .suggestedDescription(description)
                .suggestedCategory(category)
                .suggestedPriority(priority)
                .audioTranscription(transcription)
                .confidence(confidence)
                .success(true)
                .build();
    }
}
