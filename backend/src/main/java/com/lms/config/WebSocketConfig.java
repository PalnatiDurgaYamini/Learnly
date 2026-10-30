package com.lms.config;

import com.lms.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket + STOMP. The frontend connects to /ws-lms and subscribes to /topic/jobs.
 * Only a user with the ADMIN role (valid JWT sent in the STOMP CONNECT headers) may connect,
 * so ordinary students cannot receive admin job data.
 */
@Configuration @EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtUtil jwt;
    private final String[] origins;

    public WebSocketConfig(JwtUtil jwt, @Value("${app.cors-origin}") String origin) {
        this.jwt = jwt;
        this.origins = origin.split(",");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-lms").setAllowedOriginPatterns(origins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (acc != null && StompCommand.CONNECT.equals(acc.getCommand())) {
                    String h = acc.getFirstNativeHeader("Authorization");
                    try {
                        if (h == null || !h.startsWith("Bearer ")) throw new IllegalArgumentException("no token");
                        Claims c = jwt.parse(h.substring(7));
                        if (!"ADMIN".equals(c.get("role", String.class))) throw new IllegalArgumentException("not admin");
                    } catch (Exception e) {
                        throw new MessagingException("Admin login required");
                    }
                }
                return message;
            }
        });
    }
}
