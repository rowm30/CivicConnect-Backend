package com.civicconnect.api.dto.bot;

import com.civicconnect.api.entity.bot.Bot;
import com.civicconnect.api.entity.bot.Bot.BotStatus;
import com.civicconnect.api.entity.bot.Bot.BotType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for Bot entity for API responses
 */
@Data
@Builder
public class BotDTO {

    private Long id;
    private String name;
    private String description;
    private BotType botType;
    private BotStatus status;

    // Target configuration
    private String targetState;
    private String targetStateCode;
    private String sourceUrl;
    private String dataSourceName;

    // Scheduling
    private Boolean isScheduled;
    private String cronExpression;
    private LocalDateTime nextScheduledRun;

    // Stats
    private LocalDateTime lastRunAt;
    private LocalDateTime lastSuccessfulRunAt;
    private Integer totalRuns;
    private Integer successfulRuns;
    private Integer failedRuns;
    private Integer lastRecordsFetched;
    private Integer lastRecordsUpdated;
    private Integer lastRecordsInserted;

    // Error info
    private String lastErrorMessage;
    private Integer consecutiveFailures;
    private Integer maxRetries;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;

    public static BotDTO fromEntity(Bot bot) {
        return BotDTO.builder()
                .id(bot.getId())
                .name(bot.getName())
                .description(bot.getDescription())
                .botType(bot.getBotType())
                .status(bot.getStatus())
                .targetState(bot.getTargetState())
                .targetStateCode(bot.getTargetStateCode())
                .sourceUrl(bot.getSourceUrl())
                .dataSourceName(bot.getDataSourceName())
                .isScheduled(bot.getIsScheduled())
                .cronExpression(bot.getCronExpression())
                .nextScheduledRun(bot.getNextScheduledRun())
                .lastRunAt(bot.getLastRunAt())
                .lastSuccessfulRunAt(bot.getLastSuccessfulRunAt())
                .totalRuns(bot.getTotalRuns())
                .successfulRuns(bot.getSuccessfulRuns())
                .failedRuns(bot.getFailedRuns())
                .lastRecordsFetched(bot.getLastRecordsFetched())
                .lastRecordsUpdated(bot.getLastRecordsUpdated())
                .lastRecordsInserted(bot.getLastRecordsInserted())
                .lastErrorMessage(bot.getLastErrorMessage())
                .consecutiveFailures(bot.getConsecutiveFailures())
                .maxRetries(bot.getMaxRetries())
                .createdAt(bot.getCreatedAt())
                .updatedAt(bot.getUpdatedAt())
                .isActive(bot.getIsActive())
                .build();
    }
}
