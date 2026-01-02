package com.civicconnect.api.controller.bot;

import com.civicconnect.api.dto.bot.*;
import com.civicconnect.api.entity.bot.Bot;
import com.civicconnect.api.entity.bot.Bot.BotStatus;
import com.civicconnect.api.entity.bot.Bot.BotType;
import com.civicconnect.api.entity.bot.BotRun;
import com.civicconnect.api.service.bot.BotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for Bot Management (Admin Panel)
 * Endpoints for managing data scraping bots
 */
@RestController
@RequestMapping("/api/admin/bots")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BotController {

    private final BotService botService;

    // ==================== Bot CRUD ====================

    /**
     * Get all bots
     */
    @GetMapping
    public ResponseEntity<List<BotDTO>> getAllBots() {
        log.info("Getting all bots");
        List<BotDTO> bots = botService.getAllBots().stream()
                .map(BotDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(bots);
    }

    /**
     * Get bots with pagination and filters
     */
    @GetMapping("/paginated")
    public ResponseEntity<Page<BotDTO>> getBotsPaginated(
            @RequestParam(required = false) BotType type,
            @RequestParam(required = false) BotStatus status,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Getting bots paginated - type: {}, status: {}, state: {}", type, status, state);
        Page<BotDTO> bots = botService.getBotsWithFilters(type, status, state, page, size)
                .map(BotDTO::fromEntity);
        return ResponseEntity.ok(bots);
    }

    /**
     * Get bot by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BotDTO> getBotById(@PathVariable Long id) {
        log.info("Getting bot by ID: {}", id);
        return botService.getBotById(id)
                .map(bot -> ResponseEntity.ok(BotDTO.fromEntity(bot)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new bot
     */
    @PostMapping
    public ResponseEntity<BotDTO> createBot(@Valid @RequestBody CreateBotRequest request) {
        log.info("Creating bot: {}", request.getName());

        Bot bot = Bot.builder()
                .name(request.getName())
                .description(request.getDescription())
                .botType(request.getBotType())
                .targetState(request.getTargetState())
                .targetStateCode(request.getTargetStateCode())
                .sourceUrl(request.getSourceUrl())
                .dataSourceName(request.getDataSourceName())
                .isScheduled(request.getIsScheduled())
                .cronExpression(request.getCronExpression())
                .maxRetries(request.getMaxRetries())
                .configJson(request.getConfigJson())
                .build();

        Bot saved = botService.createBot(bot);
        return ResponseEntity.ok(BotDTO.fromEntity(saved));
    }

    /**
     * Update a bot
     */
    @PutMapping("/{id}")
    public ResponseEntity<BotDTO> updateBot(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBotRequest request
    ) {
        log.info("Updating bot: {}", id);

        Bot updates = Bot.builder()
                .name(request.getName())
                .description(request.getDescription())
                .targetState(request.getTargetState())
                .targetStateCode(request.getTargetStateCode())
                .sourceUrl(request.getSourceUrl())
                .dataSourceName(request.getDataSourceName())
                .isScheduled(request.getIsScheduled())
                .cronExpression(request.getCronExpression())
                .maxRetries(request.getMaxRetries())
                .configJson(request.getConfigJson())
                .build();

        Bot updated = botService.updateBot(id, updates);
        return ResponseEntity.ok(BotDTO.fromEntity(updated));
    }

    /**
     * Delete a bot
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBot(@PathVariable Long id) {
        log.info("Deleting bot: {}", id);
        botService.deleteBot(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Bot Actions ====================

    /**
     * Trigger a bot to run immediately
     */
    @PostMapping("/{id}/trigger")
    public ResponseEntity<BotRunDTO> triggerBot(@PathVariable Long id) {
        log.info("Triggering bot: {}", id);
        BotRun run = botService.triggerBot(id);
        return ResponseEntity.ok(BotRunDTO.fromEntity(run));
    }

    /**
     * Enable a bot
     */
    @PostMapping("/{id}/enable")
    public ResponseEntity<BotDTO> enableBot(@PathVariable Long id) {
        log.info("Enabling bot: {}", id);
        Bot bot = botService.enableBot(id);
        return ResponseEntity.ok(BotDTO.fromEntity(bot));
    }

    /**
     * Disable a bot
     */
    @PostMapping("/{id}/disable")
    public ResponseEntity<BotDTO> disableBot(@PathVariable Long id) {
        log.info("Disabling bot: {}", id);
        Bot bot = botService.disableBot(id);
        return ResponseEntity.ok(BotDTO.fromEntity(bot));
    }

    /**
     * Pause a bot
     */
    @PostMapping("/{id}/pause")
    public ResponseEntity<BotDTO> pauseBot(@PathVariable Long id) {
        log.info("Pausing bot: {}", id);
        Bot bot = botService.pauseBot(id);
        return ResponseEntity.ok(BotDTO.fromEntity(bot));
    }

    // ==================== Bot Runs ====================

    /**
     * Get run history for a bot
     */
    @GetMapping("/{id}/runs")
    public ResponseEntity<Page<BotRunDTO>> getBotRuns(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Getting runs for bot: {}", id);
        Page<BotRunDTO> runs = botService.getBotRuns(id, page, size)
                .map(BotRunDTO::fromEntitySummary);
        return ResponseEntity.ok(runs);
    }

    /**
     * Get latest run for a bot
     */
    @GetMapping("/{id}/runs/latest")
    public ResponseEntity<BotRunDTO> getLatestRun(@PathVariable Long id) {
        log.info("Getting latest run for bot: {}", id);
        return botService.getLatestRun(id)
                .map(run -> ResponseEntity.ok(BotRunDTO.fromEntity(run)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get currently running bots
     */
    @GetMapping("/active-runs")
    public ResponseEntity<List<BotRunDTO>> getActiveRuns() {
        log.info("Getting active runs");
        List<BotRunDTO> runs = botService.getActiveRuns().stream()
                .map(BotRunDTO::fromEntitySummary)
                .collect(Collectors.toList());
        return ResponseEntity.ok(runs);
    }

    // ==================== Dashboard ====================

    /**
     * Get dashboard statistics
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        log.info("Getting dashboard stats");
        return ResponseEntity.ok(botService.getDashboardStats());
    }

    /**
     * Get available bot types
     */
    @GetMapping("/types")
    public ResponseEntity<BotType[]> getBotTypes() {
        return ResponseEntity.ok(BotType.values());
    }

    /**
     * Get available bot statuses
     */
    @GetMapping("/statuses")
    public ResponseEntity<BotStatus[]> getBotStatuses() {
        return ResponseEntity.ok(BotStatus.values());
    }

    // ==================== Bulk Operations ====================

    /**
     * Sync all MLA-AC Sync bots for all states
     * This will link all existing MLAs to their Assembly Constituencies
     */
    @PostMapping("/sync-all-mlas")
    public ResponseEntity<Map<String, Object>> syncAllMlas() {
        log.info("Syncing all MLAs with Assembly Constituencies");
        List<Bot> syncBots = botService.getAllBots().stream()
                .filter(b -> b.getBotType() == BotType.MLA_SYNC && b.getStatus() != BotStatus.RUNNING)
                .collect(Collectors.toList());

        int triggered = 0;
        int skipped = 0;
        for (Bot bot : syncBots) {
            try {
                botService.triggerBot(bot.getId());
                triggered++;
            } catch (Exception e) {
                log.error("Failed to trigger sync bot: {}", bot.getName(), e);
                skipped++;
            }
        }

        return ResponseEntity.ok(Map.of(
                "message", "MLA sync triggered for all states",
                "botsTriggered", triggered,
                "botsSkipped", skipped
        ));
    }

    /**
     * Trigger all bots of a specific type
     */
    @PostMapping("/trigger-by-type/{type}")
    public ResponseEntity<Map<String, Object>> triggerByType(@PathVariable BotType type) {
        log.info("Triggering all bots of type: {}", type);
        List<Bot> bots = botService.getAllBots().stream()
                .filter(b -> b.getBotType() == type && b.getStatus() == BotStatus.IDLE)
                .collect(Collectors.toList());

        int triggered = 0;
        int failed = 0;
        for (Bot bot : bots) {
            try {
                botService.triggerBot(bot.getId());
                triggered++;
            } catch (Exception e) {
                log.error("Failed to trigger bot: {}", bot.getName(), e);
                failed++;
            }
        }

        return ResponseEntity.ok(Map.of(
                "message", "Triggered " + triggered + " bots of type " + type,
                "botsTriggered", triggered,
                "botsFailed", failed
        ));
    }

    // ==================== Seed ====================

    /**
     * Seed default bots (one-time setup)
     */
    @PostMapping("/seed")
    public ResponseEntity<String> seedDefaultBots() {
        log.info("Seeding default bots");
        botService.seedDefaultBots();
        return ResponseEntity.ok("Default bots seeded successfully");
    }

    /**
     * Reset and re-seed bots (deletes all existing bots and runs)
     */
    @PostMapping("/reset")
    public ResponseEntity<String> resetAndSeedBots() {
        log.info("Resetting and re-seeding bots");
        botService.resetAndSeedBots();
        return ResponseEntity.ok("Bots reset and re-seeded successfully");
    }
}
