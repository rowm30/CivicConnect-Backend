package com.civicconnect.api.controller.analytics;

import com.civicconnect.api.dto.analytics.AnalyticsDTOs.*;
import com.civicconnect.api.service.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for mobile app analytics endpoints.
 * These endpoints are called by the Android/iOS apps to send analytics data.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Start a new analytics session after user login.
     * Called when user successfully authenticates in the mobile app.
     */
    @PostMapping("/session/start")
    public ResponseEntity<SessionStartResponse> startSession(
            @RequestBody SessionStartRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("Session start request from: {}", request.getEmail());

        SessionStartResponse response = analyticsService.startSession(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    /**
     * Update session heartbeat to keep session alive.
     * Mobile app should call this every 60 seconds while active.
     */
    @PostMapping("/session/heartbeat")
    public ResponseEntity<Void> heartbeat(
            @RequestHeader("X-Session-Token") String sessionToken) {

        analyticsService.updateHeartbeat(sessionToken);
        return ResponseEntity.ok().build();
    }

    /**
     * End the current session.
     * Called when user logs out or app is closing.
     */
    @PostMapping("/session/end")
    public ResponseEntity<Void> endSession(
            @RequestHeader("X-Session-Token") String sessionToken,
            @RequestBody(required = false) SessionEndRequest request) {

        String reason = request != null ? request.getReason() : "LOGOUT";
        analyticsService.endSession(sessionToken, reason);
        return ResponseEntity.ok().build();
    }

    /**
     * Log a single activity event.
     */
    @PostMapping("/activity")
    public ResponseEntity<Void> logActivity(
            @RequestHeader("X-Session-Token") String sessionToken,
            @RequestBody ActivityLogRequest request) {

        analyticsService.logActivity(sessionToken, request);
        return ResponseEntity.accepted().build();
    }

    /**
     * Log multiple activity events in a batch.
     * More efficient than sending individual events.
     */
    @PostMapping("/activity/batch")
    public ResponseEntity<Void> logActivitiesBatch(
            @RequestHeader("X-Session-Token") String sessionToken,
            @RequestBody List<ActivityLogRequest> requests) {

        analyticsService.logActivitiesBatch(sessionToken, requests);
        return ResponseEntity.accepted().build();
    }

    /**
     * Log a single network request/response.
     */
    @PostMapping("/network")
    public ResponseEntity<Void> logNetworkRequest(
            @RequestHeader("X-Session-Token") String sessionToken,
            @RequestBody NetworkLogRequest request) {

        analyticsService.logNetworkRequest(sessionToken, request);
        return ResponseEntity.accepted().build();
    }

    /**
     * Log multiple network requests in a batch.
     * Used by the NetworkLoggingInterceptor to send buffered logs.
     */
    @PostMapping("/network/batch")
    public ResponseEntity<Void> logNetworkRequestsBatch(
            @RequestHeader("X-Session-Token") String sessionToken,
            @RequestBody List<NetworkLogRequest> requests) {

        analyticsService.logNetworkRequestsBatch(sessionToken, requests);
        return ResponseEntity.accepted().build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
