package com.civicconnect.api.repository.bot;

import com.civicconnect.api.entity.bot.BotRun;
import com.civicconnect.api.entity.bot.BotRun.RunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BotRunRepository extends JpaRepository<BotRun, Long> {

    // Find runs by bot
    Page<BotRun> findByBotIdOrderByStartedAtDesc(Long botId, Pageable pageable);

    List<BotRun> findByBotIdOrderByStartedAtDesc(Long botId);

    // Find latest run for a bot (with Bot eagerly fetched)
    @Query("SELECT br FROM BotRun br JOIN FETCH br.bot WHERE br.bot.id = :botId ORDER BY br.startedAt DESC LIMIT 1")
    Optional<BotRun> findFirstByBotIdOrderByStartedAtDesc(@Param("botId") Long botId);

    // Find runs by status
    List<BotRun> findByStatus(RunStatus status);

    // Find currently running runs
    @Query("SELECT br FROM BotRun br WHERE br.status IN ('STARTED', 'RUNNING')")
    List<BotRun> findActiveRuns();

    // Find runs within a time period
    @Query("SELECT br FROM BotRun br WHERE br.startedAt BETWEEN :start AND :end ORDER BY br.startedAt DESC")
    List<BotRun> findRunsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Find failed runs in the last N hours
    @Query("SELECT br FROM BotRun br WHERE br.status = 'FAILED' AND br.startedAt >= :since ORDER BY br.startedAt DESC")
    List<BotRun> findRecentFailedRuns(@Param("since") LocalDateTime since);

    // Statistics
    @Query("SELECT COUNT(br) FROM BotRun br WHERE br.bot.id = :botId AND br.status = :status")
    Long countByBotIdAndStatus(@Param("botId") Long botId, @Param("status") RunStatus status);

    @Query("SELECT AVG(br.durationSeconds) FROM BotRun br WHERE br.bot.id = :botId AND br.status = 'COMPLETED'")
    Double getAverageRunDuration(@Param("botId") Long botId);

    @Query("SELECT SUM(br.recordsInserted) FROM BotRun br WHERE br.bot.id = :botId AND br.status = 'COMPLETED'")
    Long getTotalRecordsInserted(@Param("botId") Long botId);

    @Query("SELECT SUM(br.recordsUpdated) FROM BotRun br WHERE br.bot.id = :botId AND br.status = 'COMPLETED'")
    Long getTotalRecordsUpdated(@Param("botId") Long botId);

    // Dashboard aggregations
    @Query("SELECT br.status, COUNT(br) FROM BotRun br WHERE br.startedAt >= :since GROUP BY br.status")
    List<Object[]> getRunStatusCountsSince(@Param("since") LocalDateTime since);

    @Query("SELECT br.bot.name, COUNT(br) FROM BotRun br WHERE br.startedAt >= :since GROUP BY br.bot.name ORDER BY COUNT(br) DESC")
    List<Object[]> getRunCountsByBotSince(@Param("since") LocalDateTime since);

    // Cleanup old runs (keep last N runs per bot)
    @Query("SELECT br.id FROM BotRun br WHERE br.bot.id = :botId ORDER BY br.startedAt DESC")
    List<Long> findRunIdsByBotIdOrderByStartedAtDesc(@Param("botId") Long botId, Pageable pageable);
}
