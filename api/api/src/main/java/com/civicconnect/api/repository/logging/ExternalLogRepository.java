package com.civicconnect.api.repository.logging;

import com.civicconnect.api.entity.logging.ExternalLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for external log persistence and queries.
 */
@Repository
public interface ExternalLogRepository extends JpaRepository<ExternalLog, Long> {

    /**
     * Find logs by source (android, admin-panel) after a certain time.
     */
    List<ExternalLog> findBySourceAndCreatedAtAfterOrderByCreatedAtDesc(String source, LocalDateTime after);

    /**
     * Find all logs with a specific correlation ID for request tracing.
     */
    List<ExternalLog> findByCorrelationIdOrderByCreatedAtAsc(String correlationId);

    /**
     * Find logs by user ID for user-specific debugging.
     */
    List<ExternalLog> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(String userId, LocalDateTime after);

    /**
     * Find logs by level (ERROR, WARN, etc.) for monitoring.
     */
    List<ExternalLog> findByLevelAndCreatedAtAfterOrderByCreatedAtDesc(String level, LocalDateTime after);

    /**
     * Count logs by level for dashboard metrics.
     */
    long countByLevelAndCreatedAtAfter(String level, LocalDateTime after);

    /**
     * Count logs by source for dashboard metrics.
     */
    long countBySourceAndCreatedAtAfter(String source, LocalDateTime after);

    /**
     * Delete logs older than a certain date for cleanup.
     */
    @Modifying
    @Query("DELETE FROM ExternalLog e WHERE e.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Find crash logs (logs with stack traces).
     */
    @Query("SELECT e FROM ExternalLog e WHERE e.stackTrace IS NOT NULL AND e.createdAt > :after ORDER BY e.createdAt DESC")
    List<ExternalLog> findCrashLogs(@Param("after") LocalDateTime after);
}
