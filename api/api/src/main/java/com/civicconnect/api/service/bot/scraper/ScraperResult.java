package com.civicconnect.api.service.bot.scraper;

import lombok.Builder;
import lombok.Data;

/**
 * Result object for scraper executions
 */
@Data
@Builder
public class ScraperResult {

    @Builder.Default
    private int recordsFetched = 0;

    @Builder.Default
    private int recordsInserted = 0;

    @Builder.Default
    private int recordsUpdated = 0;

    @Builder.Default
    private int recordsSkipped = 0;

    @Builder.Default
    private int recordsFailed = 0;

    private String logOutput;

    private boolean success;

    private String errorMessage;

    public static ScraperResult success(int fetched, int inserted, int updated, String log) {
        return ScraperResult.builder()
                .recordsFetched(fetched)
                .recordsInserted(inserted)
                .recordsUpdated(updated)
                .logOutput(log)
                .success(true)
                .build();
    }

    public static ScraperResult failure(String error, String log) {
        return ScraperResult.builder()
                .success(false)
                .errorMessage(error)
                .logOutput(log)
                .build();
    }
}
