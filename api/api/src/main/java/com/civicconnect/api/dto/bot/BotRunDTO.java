package com.civicconnect.api.dto.bot;

import com.civicconnect.api.entity.bot.BotRun;
import com.civicconnect.api.entity.bot.BotRun.RunStatus;
import com.civicconnect.api.entity.bot.BotRun.TriggerType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for BotRun entity for API responses
 */
@Data
@Builder
public class BotRunDTO {

    private Long id;
    private Long botId;
    private String botName;
    private RunStatus status;
    private TriggerType triggerType;

    // Timing
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationSeconds;

    // Progress
    private Integer totalItems;
    private Integer processedItems;
    private Integer progressPercentage;

    // Results
    private Integer recordsFetched;
    private Integer recordsInserted;
    private Integer recordsUpdated;
    private Integer recordsSkipped;
    private Integer recordsFailed;

    // Error info
    private String errorMessage;
    private Integer retryCount;

    // Log
    private String logOutput;

    public static BotRunDTO fromEntity(BotRun run) {
        return BotRunDTO.builder()
                .id(run.getId())
                .botId(run.getBot().getId())
                .botName(run.getBot().getName())
                .status(run.getStatus())
                .triggerType(run.getTriggerType())
                .startedAt(run.getStartedAt())
                .completedAt(run.getCompletedAt())
                .durationSeconds(run.getDurationSeconds())
                .totalItems(run.getTotalItems())
                .processedItems(run.getProcessedItems())
                .progressPercentage(run.getProgressPercentage())
                .recordsFetched(run.getRecordsFetched())
                .recordsInserted(run.getRecordsInserted())
                .recordsUpdated(run.getRecordsUpdated())
                .recordsSkipped(run.getRecordsSkipped())
                .recordsFailed(run.getRecordsFailed())
                .errorMessage(run.getErrorMessage())
                .retryCount(run.getRetryCount())
                .logOutput(run.getLogOutput())
                .build();
    }

    public static BotRunDTO fromEntitySummary(BotRun run) {
        // Summary without log output (for list views)
        return BotRunDTO.builder()
                .id(run.getId())
                .botId(run.getBot().getId())
                .botName(run.getBot().getName())
                .status(run.getStatus())
                .triggerType(run.getTriggerType())
                .startedAt(run.getStartedAt())
                .completedAt(run.getCompletedAt())
                .durationSeconds(run.getDurationSeconds())
                .progressPercentage(run.getProgressPercentage())
                .recordsFetched(run.getRecordsFetched())
                .recordsInserted(run.getRecordsInserted())
                .recordsUpdated(run.getRecordsUpdated())
                .recordsFailed(run.getRecordsFailed())
                .errorMessage(run.getErrorMessage())
                .build();
    }
}
