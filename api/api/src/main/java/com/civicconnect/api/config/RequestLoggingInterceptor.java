package com.civicconnect.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Interceptor that logs all incoming HTTP requests and responses.
 * Provides detailed logging for debugging, monitoring, and observability.
 */
@Component
@Slf4j
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "requestStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_ATTR, startTime);

        String queryString = request.getQueryString();
        String fullPath = request.getRequestURI() + (queryString != null ? "?" + queryString : "");

        log.info(">>> {} {} | Client: {} | User-Agent: {}",
                request.getMethod(),
                fullPath,
                getClientIp(request),
                truncate(request.getHeader("User-Agent"), 80));

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                          Object handler, ModelAndView modelAndView) {
        // No-op - logging is done in afterCompletion
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;

        if (ex != null) {
            log.error("<<< {} {} | Status: {} | Duration: {}ms | ERROR: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration,
                    ex.getMessage());
        } else {
            int status = response.getStatus();
            if (status >= 400) {
                log.warn("<<< {} {} | Status: {} | Duration: {}ms",
                        request.getMethod(),
                        request.getRequestURI(),
                        status,
                        duration);
            } else {
                log.info("<<< {} {} | Status: {} | Duration: {}ms",
                        request.getMethod(),
                        request.getRequestURI(),
                        status,
                        duration);
            }
        }
    }

    /**
     * Gets the client IP address, considering X-Forwarded-For header for proxied requests.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Truncates a string to a maximum length.
     */
    private String truncate(String value, int maxLength) {
        if (value == null) return "N/A";
        return value.length() > maxLength ? value.substring(0, maxLength) + "..." : value;
    }
}
