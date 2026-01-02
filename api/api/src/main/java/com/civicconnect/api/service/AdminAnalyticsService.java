package com.civicconnect.api.service;

import com.civicconnect.api.dto.analytics.AnalyticsDTOs;
import com.civicconnect.api.dto.analytics.AnalyticsDTOs.*;
import com.civicconnect.api.entity.analytics.*;
import com.civicconnect.api.repository.analytics.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAnalyticsService {

    private final AppUserRepository appUserRepository;
    private final UserSessionRepository sessionRepository;
    private final ActivityLogRepository activityLogRepository;
    private final NetworkLogRepository networkLogRepository;

    public Page<AppUserDTO> getAllUsers(int page, int size, String search, Boolean activeOnly) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastLoginAt").descending());

        Page<AppUser> users;
        if (Boolean.TRUE.equals(activeOnly)) {
            users = appUserRepository.searchActiveUsers(search, pageable);
        } else {
            users = appUserRepository.searchUsers(search, pageable);
        }

        return users.map(user -> AnalyticsDTOs.toDTO(user, sessionRepository.hasActiveSession(user.getId())));
    }

    public AppUserDetailDTO getUserDetail(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        List<UserSession> activeSessions = sessionRepository.findByUserIdAndIsActiveTrue(userId);
        long totalActivities = activityLogRepository.countUserActivitiesSince(userId, LocalDateTime.MIN);
        long last24hActivities = activityLogRepository.countUserActivitiesSince(userId, LocalDateTime.now().minusDays(1));

        List<ActiveSessionDTO> sessionDTOs = activeSessions.stream()
                .map(this::toActiveSessionDTO)
                .collect(Collectors.toList());

        return AppUserDetailDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .photoUrl(user.getPhotoUrl())
                .appVersion(user.getAppVersion())
                .platform(user.getPlatform() != null ? user.getPlatform().name() : null)
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .totalSessions((long) user.getSessions().size())
                .totalActivities(totalActivities)
                .activitiesLast24h(last24hActivities)
                .activeSessions(sessionDTOs)
                .build();
    }

    public Page<ActivityLogDTO> getUserActivities(Long userId, int page, int size, String activityType) {
        Pageable pageable = PageRequest.of(page, size);

        Page<ActivityLog> logs;
        if (activityType != null && !activityType.isEmpty()) {
            try {
                ActivityLog.ActivityType type = ActivityLog.ActivityType.valueOf(activityType);
                logs = activityLogRepository.findByUserIdAndType(userId, type, pageable);
            } catch (IllegalArgumentException e) {
                logs = activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            }
        } else {
            logs = activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        return logs.map(AnalyticsDTOs::toDTO);
    }

    public Page<NetworkLogDTO> getUserNetworkLogs(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NetworkLog> logs = networkLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return logs.map(log -> NetworkLogDTO.builder()
                .id(log.getId())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .requestId(log.getRequestId())
                .method(log.getMethod())
                .url(log.getUrl())
                .requestHeaders(log.getRequestHeaders())
                .requestBody(log.getRequestBody())
                .responseStatus(log.getResponseStatus())
                .responseBody(log.getResponseBody())
                .latencyMs(log.getLatencyMs())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .build());
    }

    public List<ActiveSessionDTO> getActiveSessions() {
        return sessionRepository.findActiveSessionsWithUser().stream()
                .map(this::toActiveSessionDTO)
                .collect(Collectors.toList());
    }

    public DashboardStatsDTO getDashboardStats() {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        long totalUsers = appUserRepository.count();
        long activeUsers = appUserRepository.countUsersWithActiveSessions();
        long activeSessions = sessionRepository.countActiveSessions();
        long todayActivities = activityLogRepository.countActivitiesSince(today);
        Double avgLatency = networkLogRepository.getAverageLatencySince(today);
        long todayErrors = networkLogRepository.countErrorsSince(today);

        // Calculate average session duration from active sessions
        List<UserSession> sessions = sessionRepository.findByIsActiveTrue();
        double avgDuration = sessions.stream()
                .mapToLong(s -> Duration.between(s.getStartedAt(), LocalDateTime.now()).toMinutes())
                .average()
                .orElse(0);

        return DashboardStatsDTO.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .activeSessions(activeSessions)
                .todayActivities(todayActivities)
                .avgLatencyMs(avgLatency != null ? avgLatency : 0)
                .todayErrors(todayErrors)
                .avgSessionDurationMinutes(avgDuration)
                .build();
    }

    @Transactional
    public void forceLogoutUser(Long userId) {
        int loggedOut = sessionRepository.forceLogoutUser(userId, LocalDateTime.now());
        log.info("Force logged out {} sessions for user {}", loggedOut, userId);
    }

    public List<ActivityLogDTO> getRecentActivities(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return activityLogRepository.findRecentActivitiesWithUser(pageable).stream()
                .map(AnalyticsDTOs::toDTO)
                .collect(Collectors.toList());
    }

    private ActiveSessionDTO toActiveSessionDTO(UserSession session) {
        long duration = Duration.between(session.getStartedAt(), LocalDateTime.now()).toMinutes();

        return ActiveSessionDTO.builder()
                .sessionId(session.getId())
                .userId(session.getUser().getId())
                .userName(session.getUser().getName())
                .userEmail(session.getUser().getEmail())
                .userPhotoUrl(session.getUser().getPhotoUrl())
                .deviceId(session.getDeviceId())
                .appVersion(session.getAppVersion())
                .ipAddress(session.getIpAddress())
                .startedAt(session.getStartedAt())
                .lastHeartbeatAt(session.getLastHeartbeatAt())
                .durationMinutes(duration)
                .build();
    }
}
