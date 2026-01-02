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
@Table(name = "activity_logs")
@Getter
@Setter
@NoArgsConstructor
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private UserSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Column(name = "activity_name", nullable = false)
    private String activityName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "activity_data", columnDefinition = "jsonb")
    private Map<String, Object> activityData;

    private Double latitude;

    private Double longitude;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ActivityType {
        SCREEN_VIEW,
        BUTTON_CLICK,
        API_CALL,
        LOCATION_CHANGE,
        APP_FOREGROUND,
        APP_BACKGROUND,
        LOGIN,
        LOGOUT
    }
}
