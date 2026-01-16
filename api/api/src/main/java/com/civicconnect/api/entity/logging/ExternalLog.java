package com.civicconnect.api.entity.logging;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity for storing external logs from Android/React clients.
 * Provides historical log data for queries beyond Loki retention,
 * and enables SQL-based log analysis.
 */
@Entity
@Table(name = "external_logs", indexes = {
    @Index(name = "idx_external_logs_source", columnList = "source"),
    @Index(name = "idx_external_logs_level", columnList = "level"),
    @Index(name = "idx_external_logs_correlation_id", columnList = "correlation_id"),
    @Index(name = "idx_external_logs_user_id", columnList = "user_id"),
    @Index(name = "idx_external_logs_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class ExternalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String source;  // "android" or "admin-panel"

    @Column(nullable = false, length = 10)
    private String level;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(length = 255)
    private String logger;

    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "session_id", length = 50)
    private String sessionId;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "app_version", length = 20)
    private String appVersion;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> context;

    // Android-specific
    @Column(name = "android_version", length = 20)
    private String androidVersion;

    @Column(name = "device_model", length = 100)
    private String deviceModel;

    // React-specific
    @Column(name = "browser_info", length = 255)
    private String browserInfo;

    @Column(name = "page_url", length = 500)
    private String pageUrl;

    @Column(name = "client_timestamp")
    private LocalDateTime clientTimestamp;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
