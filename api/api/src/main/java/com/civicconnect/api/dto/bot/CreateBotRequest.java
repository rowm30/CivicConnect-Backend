package com.civicconnect.api.dto.bot;

import com.civicconnect.api.entity.bot.Bot.BotType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for creating a new bot
 */
@Data
public class CreateBotRequest {

    @NotBlank(message = "Bot name is required")
    private String name;

    private String description;

    @NotNull(message = "Bot type is required")
    private BotType botType;

    @NotBlank(message = "Target state is required")
    private String targetState;

    private String targetStateCode;

    @NotBlank(message = "Source URL is required")
    private String sourceUrl;

    @NotBlank(message = "Data source name is required")
    private String dataSourceName;

    private Boolean isScheduled = false;

    private String cronExpression;

    private Integer maxRetries = 3;

    private String configJson;
}
