package com.civicconnect.api.service.bot;

import com.civicconnect.api.entity.bot.Bot;
import com.civicconnect.api.entity.bot.Bot.BotStatus;
import com.civicconnect.api.entity.bot.Bot.BotType;
import com.civicconnect.api.entity.bot.BotRun;
import com.civicconnect.api.entity.bot.BotRun.RunStatus;
import com.civicconnect.api.entity.bot.BotRun.TriggerType;
import com.civicconnect.api.repository.bot.BotRepository;
import com.civicconnect.api.repository.bot.BotRunRepository;
import com.civicconnect.api.service.bot.scraper.ScraperResult;
import com.civicconnect.api.service.bot.scraper.MlaScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing bots and their executions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BotService {

    private final BotRepository botRepository;
    private final BotRunRepository botRunRepository;
    private final MlaScraperService mlaScraperService;

    // ==================== Bot CRUD Operations ====================

    public List<Bot> getAllBots() {
        return botRepository.findAll();
    }

    public Page<Bot> getBotsPaginated(int page, int size) {
        return botRepository.findAllByOrderByNameAsc(PageRequest.of(page, size));
    }

    public Page<Bot> getBotsWithFilters(BotType type, BotStatus status, String state, int page, int size) {
        return botRepository.findWithFilters(type, status, state, PageRequest.of(page, size));
    }

    public Optional<Bot> getBotById(Long id) {
        return botRepository.findById(id);
    }

    public Optional<Bot> getBotByName(String name) {
        return botRepository.findByName(name);
    }

    @Transactional
    public Bot createBot(Bot bot) {
        bot.setStatus(BotStatus.IDLE);
        bot.setTotalRuns(0);
        bot.setSuccessfulRuns(0);
        bot.setFailedRuns(0);
        return botRepository.save(bot);
    }

    @Transactional
    public Bot updateBot(Long id, Bot updates) {
        Bot bot = botRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bot not found: " + id));

        if (updates.getName() != null) bot.setName(updates.getName());
        if (updates.getDescription() != null) bot.setDescription(updates.getDescription());
        if (updates.getTargetState() != null) bot.setTargetState(updates.getTargetState());
        if (updates.getTargetStateCode() != null) bot.setTargetStateCode(updates.getTargetStateCode());
        if (updates.getSourceUrl() != null) bot.setSourceUrl(updates.getSourceUrl());
        if (updates.getDataSourceName() != null) bot.setDataSourceName(updates.getDataSourceName());
        if (updates.getIsScheduled() != null) bot.setIsScheduled(updates.getIsScheduled());
        if (updates.getCronExpression() != null) bot.setCronExpression(updates.getCronExpression());
        if (updates.getMaxRetries() != null) bot.setMaxRetries(updates.getMaxRetries());
        if (updates.getConfigJson() != null) bot.setConfigJson(updates.getConfigJson());

        return botRepository.save(bot);
    }

    @Transactional
    public void deleteBot(Long id) {
        botRepository.deleteById(id);
    }

    // ==================== Bot Status Management ====================

    @Transactional
    public Bot enableBot(Long id) {
        Bot bot = botRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bot not found: " + id));
        bot.setStatus(BotStatus.IDLE);
        bot.setConsecutiveFailures(0);
        return botRepository.save(bot);
    }

    @Transactional
    public Bot disableBot(Long id) {
        Bot bot = botRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bot not found: " + id));
        bot.setStatus(BotStatus.DISABLED);
        return botRepository.save(bot);
    }

    @Transactional
    public Bot pauseBot(Long id) {
        Bot bot = botRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bot not found: " + id));
        bot.setStatus(BotStatus.PAUSED);
        return botRepository.save(bot);
    }

    // ==================== Bot Execution ====================

    /**
     * Trigger a bot run manually (synchronous for proper response)
     */
    @Transactional
    public BotRun triggerBot(Long botId) {
        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Bot not found: " + botId));

        if (bot.getStatus() == BotStatus.RUNNING) {
            throw new RuntimeException("Bot is already running");
        }

        if (bot.getStatus() == BotStatus.DISABLED) {
            throw new RuntimeException("Bot is disabled");
        }

        return executeBotSync(bot, TriggerType.MANUAL);
    }

    /**
     * Execute a bot synchronously and create a run record
     */
    @Transactional
    public BotRun executeBotSync(Bot bot, TriggerType triggerType) {
        log.info("Starting bot execution: {} ({})", bot.getName(), triggerType);

        // Create run record
        BotRun run = BotRun.builder()
                .bot(bot)
                .status(RunStatus.STARTED)
                .triggerType(triggerType)
                .startedAt(LocalDateTime.now())
                .sourceUrl(bot.getSourceUrl())
                .build();
        run = botRunRepository.save(run);

        // Update bot status
        bot.setStatus(BotStatus.RUNNING);
        bot.setLastRunAt(LocalDateTime.now());
        bot.setTotalRuns(bot.getTotalRuns() + 1);
        botRepository.save(bot);

        try {
            run.setStatus(RunStatus.RUNNING);
            botRunRepository.save(run);

            // Execute based on bot type
            ScraperResult result = executeBotLogic(bot, run);

            // Update run with results
            run.setRecordsFetched(result.getRecordsFetched());
            run.setRecordsInserted(result.getRecordsInserted());
            run.setRecordsUpdated(result.getRecordsUpdated());
            run.setRecordsSkipped(result.getRecordsSkipped());
            run.setRecordsFailed(result.getRecordsFailed());
            run.setLogOutput(result.getLogOutput());
            run.markCompleted();

            // Update bot stats
            bot.setStatus(BotStatus.IDLE);
            bot.setLastSuccessfulRunAt(LocalDateTime.now());
            bot.setSuccessfulRuns(bot.getSuccessfulRuns() + 1);
            bot.setConsecutiveFailures(0);
            bot.setLastRecordsFetched(result.getRecordsFetched());
            bot.setLastRecordsUpdated(result.getRecordsUpdated());
            bot.setLastRecordsInserted(result.getRecordsInserted());
            bot.setLastErrorMessage(null);

            log.info("Bot execution completed: {} - Fetched: {}, Inserted: {}, Updated: {}",
                    bot.getName(), result.getRecordsFetched(), result.getRecordsInserted(), result.getRecordsUpdated());

        } catch (Exception e) {
            log.error("Bot execution failed: {}", bot.getName(), e);

            // Get stack trace
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            run.markFailed(e.getMessage(), sw.toString());
            run.setRetryCount(run.getRetryCount() + 1);

            // Update bot error stats
            bot.setStatus(BotStatus.ERROR);
            bot.setFailedRuns(bot.getFailedRuns() + 1);
            bot.setConsecutiveFailures(bot.getConsecutiveFailures() + 1);
            bot.setLastErrorMessage(e.getMessage());

            // Check if max retries exceeded
            if (bot.getConsecutiveFailures() >= bot.getMaxRetries()) {
                log.error("Bot {} has exceeded max retries ({}), disabling", bot.getName(), bot.getMaxRetries());
            }
        }

        botRepository.save(bot);
        return botRunRepository.save(run);
    }

    /**
     * Execute a bot asynchronously (for scheduled runs)
     */
    @Async
    public void executeBotAsync(Bot bot, TriggerType triggerType) {
        executeBotSync(bot, triggerType);
    }

    /**
     * Execute the actual bot logic based on type
     */
    private ScraperResult executeBotLogic(Bot bot, BotRun run) {
        switch (bot.getBotType()) {
            case MLA_SCRAPER:
                return mlaScraperService.scrapeMLAs(bot, run);
            case MLA_SYNC:
                StringBuilder logBuilder = new StringBuilder();
                logBuilder.append("=== MLA Sync Bot Started ===\n");
                logBuilder.append("Bot: ").append(bot.getName()).append("\n");
                logBuilder.append("State: ").append(bot.getTargetState()).append("\n\n");
                return mlaScraperService.syncMlasWithConstituencies(bot.getTargetState(), logBuilder);
            case MLA_MYNETA:
                return mlaScraperService.scrapeFromMyNeta(bot, run);
            case MP_SCRAPER:
                // TODO: Implement MP scraper
                throw new UnsupportedOperationException("MP_SCRAPER not implemented yet");
            case ELECTION_RESULT:
                // TODO: Implement election result scraper
                throw new UnsupportedOperationException("ELECTION_RESULT not implemented yet");
            default:
                throw new UnsupportedOperationException("Bot type not supported: " + bot.getBotType());
        }
    }

    // ==================== Scheduled Execution ====================

    /**
     * Check for due bots every minute and execute them
     */
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void checkAndExecuteDueBots() {
        List<Bot> dueBots = botRepository.findDueBots(LocalDateTime.now());

        for (Bot bot : dueBots) {
            log.info("Scheduled execution triggered for bot: {}", bot.getName());
            try {
                executeBotAsync(bot, TriggerType.SCHEDULED);
                // Calculate next run based on cron expression
                // For simplicity, we'll set it to 24 hours later
                // In production, use CronExpression parser
                bot.setNextScheduledRun(LocalDateTime.now().plusDays(1));
                botRepository.save(bot);
            } catch (Exception e) {
                log.error("Failed to execute scheduled bot: {}", bot.getName(), e);
            }
        }
    }

    // ==================== Bot Run History ====================

    public Page<BotRun> getBotRuns(Long botId, int page, int size) {
        return botRunRepository.findByBotIdOrderByStartedAtDesc(botId, PageRequest.of(page, size));
    }

    public Optional<BotRun> getLatestRun(Long botId) {
        return botRunRepository.findFirstByBotIdOrderByStartedAtDesc(botId);
    }

    public List<BotRun> getActiveRuns() {
        return botRunRepository.findActiveRuns();
    }

    // ==================== Dashboard Statistics ====================

    public Map<String, Object> getDashboardStats() {
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);

        long totalBots = botRepository.count();
        long runningBots = botRepository.countByStatus(BotStatus.RUNNING);
        long errorBots = botRepository.countByStatus(BotStatus.ERROR);
        long disabledBots = botRepository.countByStatus(BotStatus.DISABLED);

        List<BotRun> recentFailures = botRunRepository.findRecentFailedRuns(last24Hours);
        List<Object[]> runCounts = botRunRepository.getRunStatusCountsSince(last24Hours);

        return Map.of(
                "totalBots", totalBots,
                "runningBots", runningBots,
                "errorBots", errorBots,
                "disabledBots", disabledBots,
                "recentFailures", recentFailures.size(),
                "runCountsLast24h", runCounts
        );
    }

    // ==================== Seed Default Bots ====================

    @Transactional
    public void seedDefaultBots() {
        // Check if already seeded
        if (botRepository.count() > 0) {
            log.info("Bots already exist, skipping seed");
            return;
        }

        log.info("Seeding default bots...");
        seedAllBots();
        log.info("Seeded default bots successfully");
    }

    @Transactional
    public void resetAndSeedBots() {
        log.info("Resetting all bots...");
        botRunRepository.deleteAllInBatch();
        botRepository.deleteAllInBatch();
        botRepository.flush();
        log.info("All bots deleted. Re-seeding...");
        seedAllBots();
        log.info("Re-seeded bots successfully");
    }

    private void seedAllBots() {
        // Bihar MLA Scraper (ECI - Official Source)
        createBot(Bot.builder()
                .name("Bihar MLA Scraper (ECI)")
                .description("Scrapes MLA data from Election Commission of India for Bihar Legislative Assembly Nov 2025 Results")
                .botType(BotType.MLA_SCRAPER)
                .targetState("Bihar")
                .targetStateCode("BR")
                .sourceUrl("https://results.eci.gov.in/ResultAcGenNov2025/statewiseS041.htm")
                .dataSourceName("ECI")
                .isScheduled(false)
                .status(BotStatus.IDLE)
                .build());

        // Bihar MLA Sync Bot
        createBot(Bot.builder()
                .name("Bihar MLA-AC Sync")
                .description("Syncs Bihar MLAs with Assembly Constituencies for GeoJSON mapping")
                .botType(BotType.MLA_SYNC)
                .targetState("Bihar")
                .targetStateCode("BR")
                .dataSourceName("Internal")
                .isScheduled(false)
                .status(BotStatus.IDLE)
                .build());

        // Create MLA Sync bots for major states
        createMlaSyncBot("Uttar Pradesh", "UP");
        createMlaSyncBot("Maharashtra", "MH");
        createMlaSyncBot("West Bengal", "WB");
        createMlaSyncBot("Tamil Nadu", "TN");
        createMlaSyncBot("Karnataka", "KA");
        createMlaSyncBot("Gujarat", "GJ");
        createMlaSyncBot("Madhya Pradesh", "MP");
        createMlaSyncBot("Rajasthan", "RJ");
        createMlaSyncBot("Andhra Pradesh", "AP");
        createMlaSyncBot("Telangana", "TS");
        createMlaSyncBot("Kerala", "KL");
        createMlaSyncBot("Odisha", "OD");
        createMlaSyncBot("Jharkhand", "JH");
        createMlaSyncBot("Punjab", "PB");
        createMlaSyncBot("Haryana", "HR");
        createMlaSyncBot("Chhattisgarh", "CG");
        createMlaSyncBot("Assam", "AS");
        createMlaSyncBot("Uttarakhand", "UK");
        createMlaSyncBot("Himachal Pradesh", "HP");
        createMlaSyncBot("Delhi", "DL");

        // Create MyNeta scrapers for major states
        createMyNetaBot("Bihar", "BR", "https://myneta.info/Bihar2020/index.php?action=summary&subAction=winner_type&type=elected");
        createMyNetaBot("Uttar Pradesh", "UP", "https://myneta.info/up2022/index.php?action=summary&subAction=winner_type&type=elected");
        createMyNetaBot("Gujarat", "GJ", "https://myneta.info/Gujarat2022/index.php?action=summary&subAction=winner_type&type=elected");
        createMyNetaBot("Madhya Pradesh", "MP", "https://myneta.info/MadhyaPradesh2023/index.php?action=summary&subAction=winner_type&type=elected");
        createMyNetaBot("Rajasthan", "RJ", "https://myneta.info/Rajasthan2023/index.php?action=summary&subAction=winner_type&type=elected");
        createMyNetaBot("Karnataka", "KA", "https://myneta.info/Karnataka2023/index.php?action=summary&subAction=winner_type&type=elected");
        createMyNetaBot("Telangana", "TS", "https://myneta.info/Telangana2023/index.php?action=summary&subAction=winner_type&type=elected");
        createMyNetaBot("Chhattisgarh", "CG", "https://myneta.info/Chhattisgarh2023/index.php?action=summary&subAction=winner_type&type=elected");
        createMyNetaBot("Himachal Pradesh", "HP", "https://myneta.info/HimachalPradesh2022/index.php?action=summary&subAction=winner_type&type=elected");
    }

    private void createMlaSyncBot(String state, String stateCode) {
        createBot(Bot.builder()
                .name(state + " MLA-AC Sync")
                .description("Syncs " + state + " MLAs with Assembly Constituencies for GeoJSON mapping")
                .botType(BotType.MLA_SYNC)
                .targetState(state)
                .targetStateCode(stateCode)
                .dataSourceName("Internal")
                .isScheduled(false)
                .status(BotStatus.IDLE)
                .build());
    }

    private void createMyNetaBot(String state, String stateCode, String url) {
        createBot(Bot.builder()
                .name(state + " MLA Scraper (MyNeta)")
                .description("Scrapes MLA data from MyNeta.info for " + state + " - includes criminal records, assets, education")
                .botType(BotType.MLA_MYNETA)
                .targetState(state)
                .targetStateCode(stateCode)
                .sourceUrl(url)
                .dataSourceName("MyNeta")
                .isScheduled(false)
                .status(BotStatus.IDLE)
                .build());
    }
}
