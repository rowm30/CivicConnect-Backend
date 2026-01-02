package com.civicconnect.api.entity.analytics;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "network_logs")
@Getter
@Setter
@NoArgsConstructor
public class NetworkLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private UserSession session;

    @Column(name = "request_id", length = 36)
    private String requestId;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_headers", columnDefinition = "jsonb")
    private Map<String, String> requestHeaders;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_body", columnDefinition = "jsonb")
    private Object requestBody;

    @Column(name = "response_status")
    private Integer responseStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_headers", columnDefinition = "jsonb")
    private Map<String, String> responseHeaders;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", columnDefinition = "jsonb")
    private Object responseBody;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
