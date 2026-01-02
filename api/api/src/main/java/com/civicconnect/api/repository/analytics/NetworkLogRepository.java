package com.civicconnect.api.repository.analytics;

import com.civicconnect.api.entity.analytics.NetworkLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NetworkLogRepository extends JpaRepository<NetworkLog, Long> {

    Page<NetworkLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<NetworkLog> findBySessionIdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);

    Optional<NetworkLog> findByRequestId(String requestId);

    @Query("SELECT n FROM NetworkLog n WHERE n.user.id = :userId AND n.responseStatus >= 400 ORDER BY n.createdAt DESC")
    Page<NetworkLog> findErrorsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT AVG(n.latencyMs) FROM NetworkLog n WHERE n.createdAt >= :since")
    Double getAverageLatencySince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(n) FROM NetworkLog n WHERE n.responseStatus >= 400 AND n.createdAt >= :since")
    long countErrorsSince(@Param("since") LocalDateTime since);

    @Modifying
    @Query("DELETE FROM NetworkLog n WHERE n.createdAt < :cutoff")
    int deleteOldLogs(@Param("cutoff") LocalDateTime cutoff);
}
