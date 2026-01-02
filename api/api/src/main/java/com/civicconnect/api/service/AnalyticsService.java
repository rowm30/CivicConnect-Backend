package com.civicconnect.api.service;

import com.civicconnect.api.dto.analytics.AnalyticsDTOs.*;
import com.civicconnect.api.entity.analytics.*;
import com.civicconnect.api.repository.analytics.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final AppUserRepository appUserRepository;
    private final UserSessionRepository sessionRepository;
    private final ActivityLogRepository activityLogRepository;
    private final NetworkLogRepository networkLogRepository;
    private final WebSocketEventService webSocketEventService;

    @Transactional
    public SessionStartResponse startSession(SessionStartRequest request, String ipAddress, String userAgent) {
        log.info("Starting session for user: {}", request.getEmail());

        // Find or create user
        AppUser user = appUserRepository.findByGoogleId(request.getGoogleId())
                .orElseGet(() -> {
                    AppUser newUser = new AppUser();
                    newUser.setGoogleId(request.getGoogleId());
                    newUser.setEmail(request.getEmail());
                    newUser.setName(request.getName());
                    newUser.setPhotoUrl(request.getPhotoUrl());
                    newUser.setPlatform(AppUser.Platform.valueOf(
                            request.getPlatform() != null ? request.getPlatform() : "ANDROID"));
                    return appUserRepository.save(newUser);
                });

        // Update user info
        user.setDeviceId(request.getDeviceId());
        user.setAppVersion(request.getAppVersion());
        user.setLastLoginAt(LocalDateTime.now());
        if (request.getName() != null) user.setName(request.getName());
        if (request.getPhotoUrl() != null) user.setPhotoUrl(request.getPhotoUrl());
        appUserRepository.save(user);

        // Create new session
        UserSession session = new UserSession();
        session.setUser(user);
        session.setSessionToken(UUID.randomUUID().toString());
        session.setDeviceId(request.getDeviceId());
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setAppVersion(request.getAppVersion());
        sessionRepository.save(session);

        // Log login activity
        logActivityInternal(session, ActivityLog.ActivityType.LOGIN, "User logged in", null, null, null);

        // Notify admin dashboard via WebSocket
        webSocketEventService.notifyUserOnline(user);

        return new SessionStartResponse(session.getSessionToken(), user.getId(), "Session started");
    }

    @Transactional
    public void updateHeartbeat(String sessionToken) {
        int updated = sessionRepository.updateHeartbeat(sessionToken, LocalDateTime.now());
        if (updated == 0) {
            log.warn("Heartbeat for unknown session: {}", sessionToken);
        }
    }

    @Transactional
    public void endSession(String sessionToken, String reason) {
        sessionRepository.findBySessionToken(sessionToken).ifPresent(session -> {
            session.setIsActive(false);
            session.setEndedAt(LocalDateTime.now());
            session.setEndReason(UserSession.EndReason.valueOf(reason != null ? reason : "LOGOUT"));
            sessionRepository.save(session);

            // Log logout activity
            logActivityInternal(session, ActivityLog.ActivityType.LOGOUT, "User logged out", null, null, null);

            // Check if user has any other active sessions
            if (!sessionRepository.hasActiveSession(session.getUser().getId())) {
                webSocketEventService.notifyUserOffline(session.getUser());
            }

            log.info("Session ended for user: {}", session.getUser().getEmail());
        });
    }

    @Transactional
    public void logActivity(String sessionToken, ActivityLogRequest request) {
        sessionRepository.findBySessionToken(sessionToken).ifPresent(session -> {
            ActivityLog.ActivityType type;
            try {
                type = ActivityLog.ActivityType.valueOf(request.getActivityType());
            } catch (IllegalArgumentException e) {
                type = ActivityLog.ActivityType.BUTTON_CLICK;
            }

            logActivityInternal(session, type, request.getActivityName(),
                    request.getActivityData(), request.getLatitude(), request.getLongitude());

            // Notify real-time feed
            webSocketEventService.notifyNewActivity(session.getUser(), type, request.getActivityName());
        });
    }

    @Transactional
    public void logActivitiesBatch(String sessionToken, List<ActivityLogRequest> requests) {
        sessionRepository.findBySessionToken(sessionToken).ifPresent(session -> {
            for (ActivityLogRequest request : requests) {
                ActivityLog.ActivityType type;
                try {
                    type = ActivityLog.ActivityType.valueOf(request.getActivityType());
                } catch (IllegalArgumentException e) {
                    type = ActivityLog.ActivityType.BUTTON_CLICK;
                }

                ActivityLog log = new ActivityLog();
                log.setUser(session.getUser());
                log.setSession(session);
                log.setActivityType(type);
                log.setActivityName(request.getActivityName());
                log.setActivityData(request.getActivityData());
                log.setLatitude(request.getLatitude());
                log.setLongitude(request.getLongitude());
                log.setCreatedAt(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now());
                activityLogRepository.save(log);
            }
        });
    }

    @Transactional
    public void logNetworkRequest(String sessionToken, NetworkLogRequest request) {
        sessionRepository.findBySessionToken(sessionToken).ifPresent(session -> {
            saveNetworkLog(session, request);
        });
    }

    @Transactional
    public void logNetworkRequestsBatch(String sessionToken, List<NetworkLogRequest> requests) {
        sessionRepository.findBySessionToken(sessionToken).ifPresent(session -> {
            for (NetworkLogRequest request : requests) {
                saveNetworkLog(session, request);
            }
        });
    }

    private void logActivityInternal(UserSession session, ActivityLog.ActivityType type,
                                     String name, java.util.Map<String, Object> data,
                                     Double latitude, Double longitude) {
        ActivityLog log = new ActivityLog();
        log.setUser(session.getUser());
        log.setSession(session);
        log.setActivityType(type);
        log.setActivityName(name);
        log.setActivityData(data);
        log.setLatitude(latitude);
        log.setLongitude(longitude);
        activityLogRepository.save(log);
    }

    private void saveNetworkLog(UserSession session, NetworkLogRequest request) {
        NetworkLog log = new NetworkLog();
        log.setUser(session.getUser());
        log.setSession(session);
        log.setRequestId(request.getRequestId());
        log.setMethod(request.getMethod());
        log.setUrl(request.getUrl());
        log.setRequestHeaders(sanitizeHeaders(request.getRequestHeaders()));
        log.setRequestBody(request.getRequestBody());
        log.setResponseStatus(request.getResponseStatus());
        log.setResponseHeaders(request.getResponseHeaders());
        log.setResponseBody(truncateResponseBody(request.getResponseBody()));
        log.setLatencyMs(request.getLatencyMs());
        log.setErrorMessage(request.getErrorMessage());
        log.setCreatedAt(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now());
        networkLogRepository.save(log);
    }

    private java.util.Map<String, String> sanitizeHeaders(java.util.Map<String, String> headers) {
        if (headers == null) return null;
        // Remove sensitive headers
        java.util.Map<String, String> sanitized = new java.util.HashMap<>(headers);
        sanitized.remove("Authorization");
        sanitized.remove("authorization");
        sanitized.remove("Cookie");
        sanitized.remove("cookie");
        return sanitized;
    }

    private Object truncateResponseBody(Object body) {
        if (body == null) return null;
        String json = body.toString();
        if (json.length() > 10000) {
            return java.util.Map.of("_truncated", true, "_originalSize", json.length());
        }
        return body;
    }

    @Transactional
    public void cleanupStaleSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        int expired = sessionRepository.expireStaleSessions(cutoff, LocalDateTime.now());
        if (expired > 0) {
            log.info("Expired {} stale sessions", expired);
        }
    }
}
