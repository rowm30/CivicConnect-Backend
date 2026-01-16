package com.civicconnect.api.service.logging;

import com.civicconnect.api.dto.logging.ExternalLogRequest;
import com.civicconnect.api.entity.logging.ExternalLog;
import com.civicconnect.api.repository.logging.ExternalLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Service for ingesting logs from external clients (Android, React Admin)
 * and forwarding them to Loki via the logging framework.
 */
@Service
@RequiredArgsConstructor
public class LogIngestionService {

    // Dedicated logger for external logs - picked up by Loki appender
    private static final Logger externalLogger = LoggerFactory.getLogger("EXTERNAL");

    private final ExternalLogRepository externalLogRepository;

    /**
     * Ingest a single log entry from an external client.
     */
    @Async
    public void ingestLog(ExternalLogRequest request) {
        setMdcContext(request);

        try {
            String formattedMessage = formatLogMessage(request);

            // Log to Loki via the external logger based on level
            switch (request.getLevel().toUpperCase()) {
                case "DEBUG" -> externalLogger.debug(formattedMessage);
                case "INFO" -> externalLogger.info(formattedMessage);
                case "WARN" -> externalLogger.warn(formattedMessage);
                case "ERROR" -> externalLogger.error(formattedMessage);
                default -> externalLogger.info(formattedMessage);
            }

            // Persist to database for historical queries
            persistLog(request);

        } finally {
            MDC.clear();
        }
    }

    /**
     * Ingest multiple log entries in a batch (more efficient).
     */
    @Async
    public void ingestLogBatch(List<ExternalLogRequest> requests) {
        for (ExternalLogRequest request : requests) {
            ingestLog(request);
        }
    }

    /**
     * Ingest crash/exception log with full stack trace - treated with higher priority.
     */
    @Async
    public void ingestCrashLog(ExternalLogRequest request) {
        setMdcContext(request);

        try {
            String crashMessage = formatCrashMessage(request);
            externalLogger.error(crashMessage);

            // Always persist crash logs
            persistLog(request);

        } finally {
            MDC.clear();
        }
    }

    /**
     * Get logs by correlation ID for request tracing.
     */
    public List<ExternalLog> getLogsByCorrelationId(String correlationId) {
        return externalLogRepository.findByCorrelationIdOrderByCreatedAtAsc(correlationId);
    }

    /**
     * Get recent error logs.
     */
    public List<ExternalLog> getRecentErrors(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return externalLogRepository.findByLevelAndCreatedAtAfterOrderByCreatedAtDesc("ERROR", since);
    }

    /**
     * Get recent crash logs.
     */
    public List<ExternalLog> getRecentCrashes(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return externalLogRepository.findCrashLogs(since);
    }

    /**
     * Cleanup old logs (called by scheduler).
     */
    @Transactional
    public int cleanupOldLogs(int daysToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);
        return externalLogRepository.deleteOlderThan(cutoff);
    }

    private void setMdcContext(ExternalLogRequest request) {
        if (request.getCorrelationId() != null) {
            MDC.put("correlationId", request.getCorrelationId());
        }
        if (request.getUserId() != null) {
            MDC.put("userId", request.getUserId());
        }
        MDC.put("source", request.getSource());
    }

    private String formatLogMessage(ExternalLogRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(request.getSource().toUpperCase()).append("]");

        if (request.getLogger() != null) {
            sb.append(" [").append(request.getLogger()).append("]");
        }

        sb.append(" ").append(request.getMessage());

        if (request.getContext() != null && !request.getContext().isEmpty()) {
            sb.append(" | Context: ").append(request.getContext());
        }

        return sb.toString();
    }

    private String formatCrashMessage(ExternalLogRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("[CRASH] [").append(request.getSource().toUpperCase()).append("]");

        if (request.getLogger() != null) {
            sb.append(" [").append(request.getLogger()).append("]");
        }

        sb.append(" ").append(request.getMessage());

        if (request.getStackTrace() != null) {
            sb.append("\n--- Stack Trace ---\n").append(request.getStackTrace());
        }

        if (request.getContext() != null && !request.getContext().isEmpty()) {
            sb.append("\n--- Context ---\n").append(request.getContext());
        }

        // Add device/browser info for crashes
        if (request.getDeviceModel() != null) {
            sb.append("\nDevice: ").append(request.getDeviceModel());
        }
        if (request.getAndroidVersion() != null) {
            sb.append(" (Android ").append(request.getAndroidVersion()).append(")");
        }
        if (request.getBrowserInfo() != null) {
            sb.append("\nBrowser: ").append(request.getBrowserInfo());
        }
        if (request.getPageUrl() != null) {
            sb.append("\nPage: ").append(request.getPageUrl());
        }

        return sb.toString();
    }

    @Transactional
    private void persistLog(ExternalLogRequest request) {
        ExternalLog log = new ExternalLog();
        log.setSource(request.getSource());
        log.setLevel(request.getLevel());
        log.setMessage(request.getMessage());
        log.setLogger(request.getLogger());
        log.setCorrelationId(request.getCorrelationId());
        log.setUserId(request.getUserId());
        log.setSessionId(request.getSessionId());
        log.setDeviceId(request.getDeviceId());
        log.setAppVersion(request.getAppVersion());
        log.setStackTrace(request.getStackTrace());
        log.setContext(request.getContext());
        log.setAndroidVersion(request.getAndroidVersion());
        log.setDeviceModel(request.getDeviceModel());
        log.setBrowserInfo(request.getBrowserInfo());
        log.setPageUrl(request.getPageUrl());

        if (request.getTimestamp() != null) {
            log.setClientTimestamp(LocalDateTime.ofInstant(request.getTimestamp(), ZoneId.systemDefault()));
        }

        externalLogRepository.save(log);
    }
}
