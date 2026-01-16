package com.civicconnect.api.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that extracts or generates a correlation ID for request tracing.
 * The correlation ID is stored in MDC for inclusion in all log messages,
 * enabling end-to-end request tracing across Android, React Admin, and API.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String USER_ID_MDC_KEY = "userId";
    public static final String SOURCE_MDC_KEY = "source";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Extract or generate correlation ID
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            // Extract user ID if present
            String userId = httpRequest.getHeader("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                MDC.put(USER_ID_MDC_KEY, userId);
            }

            // Determine source from headers or user agent
            String source = determineSource(httpRequest);
            MDC.put(SOURCE_MDC_KEY, source);

            // Store correlation ID in MDC
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

            // Add correlation ID to response header for client reference
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

            chain.doFilter(request, response);
        } finally {
            // Clean up MDC to prevent memory leaks
            MDC.clear();
        }
    }

    /**
     * Determines the source of the request based on headers and user agent.
     */
    private String determineSource(HttpServletRequest request) {
        // First check explicit client source header
        String clientSource = request.getHeader("X-Client-Source");
        if (clientSource != null && !clientSource.isBlank()) {
            return clientSource.toLowerCase();
        }

        // Fall back to user agent detection
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            if (userAgent.contains("Android") || userAgent.contains("okhttp")) {
                return "android";
            } else if (userAgent.contains("Mozilla") || userAgent.contains("Chrome") || userAgent.contains("Safari")) {
                return "admin-panel";
            }
        }

        return "api";
    }
}
