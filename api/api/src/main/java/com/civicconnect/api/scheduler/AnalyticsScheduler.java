package com.civicconnect.api.scheduler;

import com.civicconnect.api.repository.analytics.ActivityLogRepository;
import com.civicconnect.api.repository.analytics.NetworkLogRepository;
import com.civicconnect.api.service.AnalyticsService;
import com.civicconnect.api.service.WebSocketEventService;
import com.civicconnect.api.repository.analytics.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled tasks for analytics maintenance and real-time updates.
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class AnalyticsScheduler {

    private final AnalyticsService analyticsService;
    private final WebSocketEventService webSocketEventService;
    private final UserSessionRepository sessionRepository;
    private final ActivityLogRepository activityLogRepository;
    private final NetworkLogRepository networkLogRepository;

    /**
     * Check for stale sessions every minute.
     * Sessions without heartbeat for 5 minutes are marked as inactive.
     */
    @Scheduled(fixedRate = 60000) // Every 1 minute
    public void cleanupStaleSessions() {
        analyticsService.cleanupStaleSessions();
    }

    /**
     * Broadcast active user count every 30 seconds.
     * Admin dashboard subscribes to this for real-time count updates.
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void broadcastActiveUserCount() {
        long count = sessionRepository.countActiveSessions();
        webSocketEventService.broadcastActiveUserCount(count);
    }

    /**
     * Cleanup old logs daily at 3 AM.
     * - Network logs older than 7 days
     * - Activity logs older than 30 days
     */
    @Scheduled(cron = "0 0 3 * * *") // 3 AM daily
    @Transactional
    public void cleanupOldLogs() {
        log.info("Starting daily log cleanup...");

        // Delete network logs older than 7 days
        LocalDateTime networkCutoff = LocalDateTime.now().minusDays(7);
        int deletedNetworkLogs = networkLogRepository.deleteOldLogs(networkCutoff);
        log.info("Deleted {} network logs older than 7 days", deletedNetworkLogs);

        // Delete activity logs older than 30 days
        LocalDateTime activityCutoff = LocalDateTime.now().minusDays(30);
        int deletedActivityLogs = activityLogRepository.deleteOldLogs(activityCutoff);
        log.info("Deleted {} activity logs older than 30 days", deletedActivityLogs);

        log.info("Daily log cleanup completed");
    }
}
