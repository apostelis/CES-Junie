package com.ces.infrastructure.config;

import com.ces.domain.model.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration for Redis-based session storage.
 * Only activated when Redis is enabled via configuration.
 */
@Configuration
@ConditionalOnProperty(name = "ces.session.storage", havingValue = "redis")
public class RedisConfiguration {

    /**
     * Configures RedisTemplate for Session storage.
     * Uses JSON serialization for Session objects.
     *
     * @param connectionFactory Redis connection factory auto-configured by Spring Boot
     * @return configured RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Session> sessionRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Session> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for Session values
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        template.afterPropertiesSet();
        return template;
    }
}
