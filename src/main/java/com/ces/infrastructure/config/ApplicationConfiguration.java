package com.ces.infrastructure.config;

import com.ces.application.port.input.DeliverMessageUseCase;
import com.ces.application.port.input.RegisterSessionUseCase;
import com.ces.application.port.output.MessageSender;
import com.ces.application.port.output.SessionRepository;
import com.ces.application.service.DeliverMessageService;
import com.ces.application.service.RegisterSessionService;
import com.ces.domain.model.Session;
import com.ces.domain.service.SessionRegistry;
import com.ces.infrastructure.adapter.InMemorySessionRegistry;
import com.ces.infrastructure.adapter.InMemorySessionRepository;
import com.ces.infrastructure.adapter.RedisSessionRegistry;
import com.ces.infrastructure.adapter.WebSocketMessageSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Configuration class for application layer beans.
 * This keeps the application layer free from Spring framework dependencies,
 * adhering to Hexagonal Architecture principles where the application layer
 * should be framework-agnostic.
 */
@Configuration
public class ApplicationConfiguration {

    /**
     * In-memory SessionRegistry for local development.
     * Activated when ces.session.storage is not set to "redis" or not present.
     */
    @Bean
    @ConditionalOnProperty(name = "ces.session.storage", havingValue = "in-memory", matchIfMissing = true)
    public SessionRegistry inMemorySessionRegistry() {
        return new InMemorySessionRegistry();
    }

    /**
     * Redis-based SessionRegistry for production environments.
     * Activated when ces.session.storage is set to "redis".
     */
    @Bean
    @ConditionalOnProperty(name = "ces.session.storage", havingValue = "redis")
    public SessionRegistry redisSessionRegistry(RedisTemplate<String, Session> sessionRedisTemplate) {
        return new RedisSessionRegistry(sessionRedisTemplate);
    }

    @Bean
    public MessageSender messageSender() {
        return new WebSocketMessageSender();
    }

    @Bean
    public SessionRepository sessionRepository() {
        return new InMemorySessionRepository();
    }

    @Bean
    public DeliverMessageUseCase deliverMessageUseCase(
            SessionRegistry sessionRegistry,
            MessageSender messageSender) {
        return new DeliverMessageService(sessionRegistry, messageSender);
    }

    @Bean
    public RegisterSessionUseCase registerSessionUseCase(
            SessionRegistry sessionRegistry,
            SessionRepository sessionRepository,
            @Value("${ces.websocket.base-url}") String websocketBaseUrl) {
        return new RegisterSessionService(sessionRegistry, sessionRepository, websocketBaseUrl);
    }
}
