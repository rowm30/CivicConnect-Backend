package com.civicconnect.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class JavaCodeExtractionDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractionRequest {
        private Long userId;
        private String sessionName;
        private List<String> imageUrls; // URLs of uploaded images
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractionResponse {
        private Long sessionId;
        private String sessionName;
        private String status; // PENDING, PROCESSING, COMPLETED, FAILED
        private int totalImages;
        private int processedImages;
        private String extractedCode;
        private String errorMessage;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionListResponse {
        private Long id;
        private String sessionName;
        private String status;
        private int totalImages;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageUploadResponse {
        private String imageUrl;
        private int imageIndex;
        private boolean success;
        private String errorMessage;
    }
}
