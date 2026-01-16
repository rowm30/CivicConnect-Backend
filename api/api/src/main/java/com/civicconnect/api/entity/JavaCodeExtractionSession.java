package com.civicconnect.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "java_code_extraction_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JavaCodeExtractionSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_name")
    private String sessionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ExtractionStatus status;

    @Column(name = "total_images")
    private int totalImages;

    @Column(name = "processed_images")
    private int processedImages;

    @Column(name = "extracted_code", columnDefinition = "TEXT")
    private String extractedCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum ExtractionStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ExtractionStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
