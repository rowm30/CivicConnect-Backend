package com.civicconnect.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time admin dashboard updates.
 * Uses STOMP over WebSocket with SockJS fallback.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple in-memory broker for broadcasting to subscribers
        // Clients subscribe to /topic/* to receive updates
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages FROM clients TO server (if needed)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint that admin dashboard connects to
        // SockJS provides fallback for browsers without WebSocket support
        registry.addEndpoint("/ws/admin")
                .setAllowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:3000",
                        "http://127.0.0.1:5173",
                        "http://127.0.0.1:3000"
                )
                .withSockJS();
    }
}
