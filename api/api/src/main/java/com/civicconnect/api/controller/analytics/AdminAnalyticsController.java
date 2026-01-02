package com.civicconnect.api.controller.analytics;

import com.civicconnect.api.dto.analytics.AnalyticsDTOs.*;
import com.civicconnect.api.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for admin dashboard analytics endpoints.
 * These endpoints are called by the admin web portal.
 */
@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminService;

    /**
     * Get all registered app users with pagination and search.
     */
    @GetMapping("/users")
    public ResponseEntity<Page<AppUserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activeOnly) {

        return ResponseEntity.ok(adminService.getAllUsers(page, size, search, activeOnly));
    }

    /**
     * Get detailed information about a specific user.
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<AppUserDetailDTO> getUserDetail(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getUserDetail(userId));
    }

    /**
     * Get activity logs for a specific user.
     */
    @GetMapping("/users/{userId}/activities")
    public ResponseEntity<Page<ActivityLogDTO>> getUserActivities(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String activityType) {

        return ResponseEntity.ok(adminService.getUserActivities(userId, page, size, activityType));
    }

    /**
     * Get network logs for a specific user.
     */
    @GetMapping("/users/{userId}/network-logs")
    public ResponseEntity<Page<NetworkLogDTO>> getUserNetworkLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        return ResponseEntity.ok(adminService.getUserNetworkLogs(userId, page, size));
    }

    /**
     * Get all currently active sessions.
     */
    @GetMapping("/active-sessions")
    public ResponseEntity<List<ActiveSessionDTO>> getActiveSessions() {
        return ResponseEntity.ok(adminService.getActiveSessions());
    }

    /**
     * Get dashboard statistics.
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    /**
     * Get recent activities for the live feed.
     */
    @GetMapping("/recent-activities")
    public ResponseEntity<List<ActivityLogDTO>> getRecentActivities(
            @RequestParam(defaultValue = "50") int limit) {

        return ResponseEntity.ok(adminService.getRecentActivities(limit));
    }

    /**
     * Force logout a user (terminate all their active sessions).
     */
    @DeleteMapping("/users/{userId}/sessions")
    public ResponseEntity<Void> forceLogoutUser(@PathVariable Long userId) {
        log.info("Force logout requested for user: {}", userId);
        adminService.forceLogoutUser(userId);
        return ResponseEntity.noContent().build();
    }
}
