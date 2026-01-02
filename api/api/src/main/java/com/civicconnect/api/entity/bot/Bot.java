package com.civicconnect.api.entity.bot;

import com.civicconnect.api.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a data scraping bot
 * Bots are responsible for fetching and updating data from external sources
 */
@Entity
@Table(name = "bots", indexes = {
        @Index(name = "idx_bot_type", columnList = "bot_type"),
        @Index(name = "idx_bot_state", columnList = "target_state"),
        @Index(name = "idx_bot_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bot extends BaseEntity {

    @NotBlank(message = "Bot name is required")
    @Size(max = 100)
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Size(max = 500)
    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "bot_type", nullable = false)
    private BotType botType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private BotStatus status = BotStatus.IDLE;

    // Target configuration
    @Size(max = 100)
    @Column(name = "target_state")
    private String targetState;  // e.g., "Bihar", "UP", "Maharashtra"

    @Size(max = 10)
    @Column(name = "target_state_code")
    private String targetStateCode;

    @Size(max = 500)
    @Column(name = "source_url")
    private String sourceUrl;  // Primary data source URL

    @Size(max = 100)
    @Column(name = "data_source_name")
    private String dataSourceName;  // MyNeta, ECI, State Website, etc.

    // Scheduling configuration
    @Column(name = "is_scheduled")
    @Builder.Default
    private Boolean isScheduled = false;

    @Size(max = 50)
    @Column(name = "cron_expression")
    private String cronExpression;  // e.g., "0 0 2 * * ?" for 2 AM daily

    @Column(name = "next_scheduled_run")
    private LocalDateTime nextScheduledRun;

    // Execution stats
    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "last_successful_run_at")
    private LocalDateTime lastSuccessfulRunAt;

    @Column(name = "total_runs")
    @Builder.Default
    private Integer totalRuns = 0;

    @Column(name = "successful_runs")
    @Builder.Default
    private Integer successfulRuns = 0;

    @Column(name = "failed_runs")
    @Builder.Default
    private Integer failedRuns = 0;

    @Column(name = "last_records_fetched")
    @Builder.Default
    private Integer lastRecordsFetched = 0;

    @Column(name = "last_records_updated")
    @Builder.Default
    private Integer lastRecordsUpdated = 0;

    @Column(name = "last_records_inserted")
    @Builder.Default
    private Integer lastRecordsInserted = 0;

    // Error tracking
    @Size(max = 1000)
    @Column(name = "last_error_message")
    private String lastErrorMessage;

    @Column(name = "consecutive_failures")
    @Builder.Default
    private Integer consecutiveFailures = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    // Configuration JSON (flexible settings)
    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;  // Additional bot-specific configuration

    // Bot runs history
    @OneToMany(mappedBy = "bot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BotRun> runs = new ArrayList<>();

    public enum BotType {
        MLA_SCRAPER,          // Scrapes MLA data from various sources
        MLA_SYNC,             // Syncs existing MLAs with Assembly Constituencies
        MLA_MYNETA,           // Scrapes MLA data from MyNeta.info
        MP_SCRAPER,           // Scrapes MP data
        ELECTION_RESULT,      // Fetches election results from ECI
        CONSTITUENCY_DATA,    // Updates constituency boundaries/data
        BENEFIT_SCRAPER,      // Scrapes government benefit schemes
        NEWS_AGGREGATOR       // Aggregates political news
    }

    public enum BotStatus {
        IDLE,           // Not running, waiting for next scheduled run
        RUNNING,        // Currently executing
        PAUSED,         // Manually paused
        DISABLED,       // Disabled by admin
        ERROR,          // In error state (after max retries)
        COMPLETED       // Just completed a run
    }
}
