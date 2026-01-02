package com.civicconnect.api.repository.analytics;

import com.civicconnect.api.entity.analytics.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Page<ActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<ActivityLog> findBySessionIdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);

    @Query("SELECT a FROM ActivityLog a WHERE a.user.id = :userId AND a.activityType = :type ORDER BY a.createdAt DESC")
    Page<ActivityLog> findByUserIdAndType(@Param("userId") Long userId,
                                          @Param("type") ActivityLog.ActivityType type,
                                          Pageable pageable);

    @Query("SELECT a FROM ActivityLog a JOIN FETCH a.user ORDER BY a.createdAt DESC")
    List<ActivityLog> findRecentActivitiesWithUser(Pageable pageable);

    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.createdAt >= :since")
    long countActivitiesSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.user.id = :userId AND a.createdAt >= :since")
    long countUserActivitiesSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Modifying
    @Query("DELETE FROM ActivityLog a WHERE a.createdAt < :cutoff")
    int deleteOldLogs(@Param("cutoff") LocalDateTime cutoff);
}
