package com.civicconnect.api.controller;

import com.civicconnect.api.dto.logging.ExternalLogRequest;
import com.civicconnect.api.entity.logging.ExternalLog;
import com.civicconnect.api.service.logging.LogIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for ingesting logs from external clients (Android, React Admin).
 * These logs are forwarded to Loki for centralized storage and also persisted
 * to the database for SQL-based queries.
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LogIngestionController {

    private final LogIngestionService logIngestionService;

    /**
     * Ingest a single log entry from an external client.
     */
    @PostMapping
    public ResponseEntity<Void> ingestLog(@Valid @RequestBody ExternalLogRequest request) {
        logIngestionService.ingestLog(request);
        return ResponseEntity.accepted().build();
    }

    /**
     * Ingest multiple log entries in a batch (more efficient for mobile clients).
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> ingestLogBatch(@Valid @RequestBody List<ExternalLogRequest> requests) {
        logIngestionService.ingestLogBatch(requests);
        return ResponseEntity.accepted().body(Map.of(
                "accepted", requests.size(),
                "message", "Logs queued for processing"
        ));
    }

    /**
     * Ingest crash/exception log with full stack trace.
     * This endpoint treats crashes with higher priority.
     */
    @PostMapping("/crash")
    public ResponseEntity<Void> ingestCrashLog(@Valid @RequestBody ExternalLogRequest request) {
        log.warn("Crash report received from {} - {}", request.getSource(), request.getMessage());
        logIngestionService.ingestCrashLog(request);
        return ResponseEntity.accepted().build();
    }

    /**
     * Get logs by correlation ID for request tracing.
     * Useful for debugging a specific user request across all components.
     */
    @GetMapping("/trace/{correlationId}")
    public ResponseEntity<List<ExternalLog>> getLogsByCorrelationId(@PathVariable String correlationId) {
        List<ExternalLog> logs = logIngestionService.getLogsByCorrelationId(correlationId);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get recent error logs for monitoring.
     */
    @GetMapping("/errors")
    public ResponseEntity<List<ExternalLog>> getRecentErrors(
            @RequestParam(defaultValue = "24") int hours) {
        List<ExternalLog> errors = logIngestionService.getRecentErrors(hours);
        return ResponseEntity.ok(errors);
    }

    /**
     * Get recent crash logs.
     */
    @GetMapping("/crashes")
    public ResponseEntity<List<ExternalLog>> getRecentCrashes(
            @RequestParam(defaultValue = "24") int hours) {
        List<ExternalLog> crashes = logIngestionService.getRecentCrashes(hours);
        return ResponseEntity.ok(crashes);
    }
}
