package com.civicconnect.api.repository.analytics;

import com.civicconnect.api.entity.analytics.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findBySessionToken(String sessionToken);

    List<UserSession> findByUserIdAndIsActiveTrue(Long userId);

    List<UserSession> findByIsActiveTrue();

    @Query("SELECT s FROM UserSession s JOIN FETCH s.user WHERE s.isActive = true")
    List<UserSession> findActiveSessionsWithUser();

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.isActive = true")
    long countActiveSessions();

    @Modifying
    @Query("UPDATE UserSession s SET s.lastHeartbeatAt = :timestamp WHERE s.sessionToken = :token")
    int updateHeartbeat(@Param("token") String sessionToken, @Param("timestamp") LocalDateTime timestamp);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false, s.endedAt = :timestamp, s.endReason = 'TIMEOUT' " +
           "WHERE s.isActive = true AND s.lastHeartbeatAt < :cutoff")
    int expireStaleSessions(@Param("cutoff") LocalDateTime cutoff, @Param("timestamp") LocalDateTime timestamp);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false, s.endedAt = :timestamp, s.endReason = 'FORCE_LOGOUT' " +
           "WHERE s.user.id = :userId AND s.isActive = true")
    int forceLogoutUser(@Param("userId") Long userId, @Param("timestamp") LocalDateTime timestamp);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM UserSession s " +
           "WHERE s.user.id = :userId AND s.isActive = true")
    boolean hasActiveSession(@Param("userId") Long userId);
}
