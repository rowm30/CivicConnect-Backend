package com.civicconnect.api.entity.bot;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.Duration;

/**
 * Entity representing a single execution run of a bot
 * Tracks detailed information about each bot run for auditing and debugging
 */
@Entity
@Table(name = "bot_runs", indexes = {
        @Index(name = "idx_bot_run_bot_id", columnList = "bot_id"),
        @Index(name = "idx_bot_run_status", columnList = "status"),
        @Index(name = "idx_bot_run_started", columnList = "started_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BotRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot bot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RunStatus status = RunStatus.STARTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    @Builder.Default
    private TriggerType triggerType = TriggerType.MANUAL;

    // Timing
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    // Progress tracking
    @Column(name = "total_items")
    @Builder.Default
    private Integer totalItems = 0;

    @Column(name = "processed_items")
    @Builder.Default
    private Integer processedItems = 0;

    @Column(name = "progress_percentage")
    @Builder.Default
    private Integer progressPercentage = 0;

    // Results
    @Column(name = "records_fetched")
    @Builder.Default
    private Integer recordsFetched = 0;

    @Column(name = "records_inserted")
    @Builder.Default
    private Integer recordsInserted = 0;

    @Column(name = "records_updated")
    @Builder.Default
    private Integer recordsUpdated = 0;

    @Column(name = "records_skipped")
    @Builder.Default
    private Integer recordsSkipped = 0;

    @Column(name = "records_failed")
    @Builder.Default
    private Integer recordsFailed = 0;

    // Error information
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    private String errorStackTrace;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    // Logs
    @Column(name = "log_output", columnDefinition = "TEXT")
    private String logOutput;  // Detailed execution log

    // Metadata
    @Column(name = "source_url")
    private String sourceUrl;  // URL that was scraped

    @Column(name = "run_config_json", columnDefinition = "TEXT")
    private String runConfigJson;  // Configuration used for this run

    public void markCompleted() {
        this.status = RunStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        if (this.startedAt != null) {
            this.durationSeconds = Duration.between(this.startedAt, this.completedAt).getSeconds();
        }
        this.progressPercentage = 100;
    }

    public void markFailed(String errorMessage, String stackTrace) {
        this.status = RunStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        if (this.startedAt != null) {
            this.durationSeconds = Duration.between(this.startedAt, this.completedAt).getSeconds();
        }
        this.errorMessage = errorMessage;
        this.errorStackTrace = stackTrace;
    }

    public void updateProgress(int processed, int total) {
        this.processedItems = processed;
        this.totalItems = total;
        if (total > 0) {
            this.progressPercentage = (processed * 100) / total;
        }
    }

    public enum RunStatus {
        STARTED,        // Run has started
        RUNNING,        // Currently processing
        COMPLETED,      // Successfully completed
        FAILED,         // Failed with error
        CANCELLED,      // Manually cancelled
        TIMEOUT         // Exceeded maximum run time
    }

    public enum TriggerType {
        MANUAL,         // Manually triggered from admin panel
        SCHEDULED,      // Triggered by scheduler
        WEBHOOK,        // Triggered by external webhook
        RETRY           // Retry of a failed run
    }
}
