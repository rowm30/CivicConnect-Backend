package com.civicconnect.api.dto.analytics;

import com.civicconnect.api.entity.analytics.ActivityLog;
import com.civicconnect.api.entity.analytics.AppUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AnalyticsDTOs {

    // ============ Request DTOs (from Mobile App) ============

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionStartRequest {
        private String googleId;
        private String email;
        private String name;
        private String photoUrl;
        private String deviceId;
        private String appVersion;
        private String platform;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionStartResponse {
        private String sessionToken;
        private Long userId;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionEndRequest {
        private String reason; // LOGOUT, APP_KILLED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityLogRequest {
        private String activityType; // SCREEN_VIEW, BUTTON_CLICK, etc.
        private String activityName;
        private Map<String, Object> activityData;
        private Double latitude;
        private Double longitude;
        private LocalDateTime timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NetworkLogRequest {
        private String requestId;
        private String method;
        private String url;
        private Map<String, String> requestHeaders;
        private Object requestBody;
        private Integer responseStatus;
        private Map<String, String> responseHeaders;
        private Object responseBody;
        private Integer latencyMs;
        private String errorMessage;
        private LocalDateTime timestamp;
    }

    // ============ Response DTOs (for Admin Dashboard) ============

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppUserDTO {
        private Long id;
        private String googleId;
        private String email;
        private String name;
        private String photoUrl;
        private String deviceId;
        private String appVersion;
        private String platform;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginAt;
        private Boolean isActive;
        private Boolean hasActiveSession;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppUserDetailDTO {
        private Long id;
        private String email;
        private String name;
        private String photoUrl;
        private String appVersion;
        private String platform;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginAt;
        private Long totalSessions;
        private Long totalActivities;
        private Long activitiesLast24h;
        private List<ActiveSessionDTO> activeSessions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveSessionDTO {
        private Long sessionId;
        private Long userId;
        private String userName;
        private String userEmail;
        private String userPhotoUrl;
        private String deviceId;
        private String appVersion;
        private String ipAddress;
        private LocalDateTime startedAt;
        private LocalDateTime lastHeartbeatAt;
        private Long durationMinutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityLogDTO {
        private Long id;
        private Long userId;
        private String userName;
        private String activityType;
        private String activityName;
        private Map<String, Object> activityData;
        private Double latitude;
        private Double longitude;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NetworkLogDTO {
        private Long id;
        private Long userId;
        private String requestId;
        private String method;
        private String url;
        private Map<String, String> requestHeaders;
        private Object requestBody;
        private Integer responseStatus;
        private Object responseBody;
        private Integer latencyMs;
        private String errorMessage;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardStatsDTO {
        private Long totalUsers;
        private Long activeUsers;
        private Long activeSessions;
        private Long todayActivities;
        private Double avgLatencyMs;
        private Long todayErrors;
        private Double avgSessionDurationMinutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatusUpdate {
        private Long userId;
        private String email;
        private String name;
        private String photoUrl;
        private Boolean isActive;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityUpdate {
        private Long userId;
        private String userName;
        private String userPhotoUrl;
        private String activityType;
        private String activityName;
        private LocalDateTime timestamp;
    }

    // ============ Mapper Methods ============

    public static AppUserDTO toDTO(AppUser user, boolean hasActiveSession) {
        return AppUserDTO.builder()
                .id(user.getId())
                .googleId(user.getGoogleId())
                .email(user.getEmail())
                .name(user.getName())
                .photoUrl(user.getPhotoUrl())
                .deviceId(user.getDeviceId())
                .appVersion(user.getAppVersion())
                .platform(user.getPlatform() != null ? user.getPlatform().name() : null)
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .isActive(user.getIsActive())
                .hasActiveSession(hasActiveSession)
                .build();
    }

    public static ActivityLogDTO toDTO(ActivityLog log) {
        return ActivityLogDTO.builder()
                .id(log.getId())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .userName(log.getUser() != null ? log.getUser().getName() : null)
                .activityType(log.getActivityType() != null ? log.getActivityType().name() : null)
                .activityName(log.getActivityName())
                .activityData(log.getActivityData())
                .latitude(log.getLatitude())
                .longitude(log.getLongitude())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
