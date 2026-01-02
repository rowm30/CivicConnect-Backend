package com.civicconnect.api.entity.analytics;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "session_token", unique = true, nullable = false)
    private String sessionToken;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "started_at")
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt = LocalDateTime.now();

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason", length = 50)
    private EndReason endReason;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ActivityLog> activities = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<NetworkLog> networkLogs = new ArrayList<>();

    public enum EndReason {
        LOGOUT, TIMEOUT, APP_KILLED, FORCE_LOGOUT
    }
}
