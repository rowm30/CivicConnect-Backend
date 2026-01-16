package com.civicconnect.api.dto.logging;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for external log entries from Android/React clients.
 * This allows centralized log aggregation from all application components.
 */
@Data
public class ExternalLogRequest {

    @NotBlank(message = "Source is required")
    private String source;  // "android" or "admin-panel"

    @NotBlank(message = "Level is required")
    private String level;   // DEBUG, INFO, WARN, ERROR

    @NotBlank(message = "Message is required")
    private String message;

    private String logger;  // Component/class name

    private String correlationId;

    private String userId;

    private String sessionId;

    private String deviceId;

    private String appVersion;

    private Instant timestamp;

    private String stackTrace;  // For errors/crashes

    private Map<String, Object> context;  // Additional context data

    // Android-specific fields
    private String androidVersion;
    private String deviceModel;

    // React-specific fields
    private String browserInfo;
    private String pageUrl;
}
