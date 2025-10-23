package org.bithub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configures WebSocket messaging for real-time communication.
 * Enables STOMP protocol support and sets up the message broker
 * and WebSocket endpoints used by clients.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configures the message broker for routing messages between
     * clients and the server.
     *
     * "/topic" is used for broadcasting messages to subscribed clients,
     * while "/app" is used as a prefix for client-to-server communication.
     *
     * @param config the message broker configuration
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registers the WebSocket endpoint used by clients to connect
     * to the server. SockJS is enabled to support fallback options
     * for browsers that do not support native WebSocket.
     *
     * @param registry the STOMP endpoint registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                .withSockJS();
    }
}
