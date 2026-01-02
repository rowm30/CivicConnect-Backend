package com.civicconnect.api.dto.bot;

import lombok.Data;

/**
 * Request DTO for updating an existing bot
 */
@Data
public class UpdateBotRequest {

    private String name;
    private String description;
    private String targetState;
    private String targetStateCode;
    private String sourceUrl;
    private String dataSourceName;
    private Boolean isScheduled;
    private String cronExpression;
    private Integer maxRetries;
    private String configJson;
}
