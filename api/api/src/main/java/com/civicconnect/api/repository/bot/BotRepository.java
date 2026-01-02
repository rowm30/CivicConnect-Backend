package com.civicconnect.api.repository.bot;

import com.civicconnect.api.entity.bot.Bot;
import com.civicconnect.api.entity.bot.Bot.BotStatus;
import com.civicconnect.api.entity.bot.Bot.BotType;
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
public interface BotRepository extends JpaRepository<Bot, Long> {

    Optional<Bot> findByName(String name);

    List<Bot> findByBotType(BotType botType);

    List<Bot> findByStatus(BotStatus status);

    List<Bot> findByTargetState(String targetState);

    List<Bot> findByIsScheduledTrue();

    // Find bots that need to run (scheduled and due)
    @Query("SELECT b FROM Bot b WHERE b.isScheduled = true AND b.status != 'DISABLED' " +
           "AND b.nextScheduledRun <= :now")
    List<Bot> findDueBots(@Param("now") LocalDateTime now);

    // Find active bots (not disabled)
    @Query("SELECT b FROM Bot b WHERE b.status != 'DISABLED' ORDER BY b.name")
    List<Bot> findActiveBots();

    // Find bots by type and state
    List<Bot> findByBotTypeAndTargetState(BotType botType, String targetState);

    // Statistics queries
    @Query("SELECT COUNT(b) FROM Bot b WHERE b.status = :status")
    Long countByStatus(@Param("status") BotStatus status);

    @Query("SELECT b FROM Bot b WHERE b.status = 'ERROR' AND b.consecutiveFailures >= :threshold")
    List<Bot> findBotsWithCriticalErrors(@Param("threshold") int threshold);

    // Paginated list for admin panel
    Page<Bot> findAllByOrderByNameAsc(Pageable pageable);

    @Query("SELECT b FROM Bot b WHERE " +
           "(:type IS NULL OR b.botType = :type) AND " +
           "(:status IS NULL OR b.status = :status) AND " +
           "(:state IS NULL OR b.targetState = :state) " +
           "ORDER BY b.name")
    Page<Bot> findWithFilters(
            @Param("type") BotType type,
            @Param("status") BotStatus status,
            @Param("state") String state,
            Pageable pageable
    );
}
