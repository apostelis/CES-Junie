# Redis Session Storage

## Overview

The Customer Event Stream application uses Redis for distributed session storage in production environments, while maintaining an in-memory implementation for local development. This allows the application to scale horizontally while managing WebSocket subscriptions across multiple instances.

## Architecture

### Session Storage Implementations

The application provides two implementations of the `SessionRegistry` interface:

1. **InMemorySessionRegistry**: Uses a `ConcurrentHashMap` for local storage (development)
2. **RedisSessionRegistry**: Uses Redis for distributed storage (production)

Both implementations support:
- Targeted messages to specific sessions
- Broadcasting messages to all active sessions
- Session lifecycle management (register, update heartbeat, remove)
- Automatic expiration of stale sessions

### Configuration

Session storage is controlled by the `ces.session.storage` property:

- `in-memory`: Uses InMemorySessionRegistry (default for dev)
- `redis`: Uses RedisSessionRegistry (production)

## Development Setup

### Local Development (In-Memory)

For local development, sessions are stored in memory. No additional setup required.

**Configuration** (`application-dev.yml`):
```yaml
ces:
  session:
    storage: in-memory
```

### Local Development with Redis (Optional)

To test Redis integration locally:

1. Start Redis using Docker:
```bash
docker run -d --name redis-local -p 6379:6379 redis:7-alpine
```

2. Update `application-dev.yml`:
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms

ces:
  session:
    storage: redis
```

## Production Setup

### Redis Configuration

**Configuration** (`application-prod.yml`):
```yaml
spring:
  data:
    redis:
      host: redis-cluster.example.com
      port: 6379
      password: ${REDIS_PASSWORD}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5

ces:
  session:
    storage: redis
```

### Redis Deployment

For production, use a managed Redis service or deploy Redis cluster:

#### Using Redis Cluster
```bash
# Example Redis cluster with persistence
docker run -d \
  --name redis-ces \
  -p 6379:6379 \
  -v redis-data:/data \
  -e REDIS_PASSWORD=your-secure-password \
  redis:7-alpine \
  redis-server --appendonly yes --requirepass your-secure-password
```

#### Using Cloud Services
- **AWS ElastiCache for Redis**: Fully managed Redis service
- **Azure Cache for Redis**: Managed Redis in Azure
- **Google Cloud Memorystore**: Redis on Google Cloud

### Environment Variables

Set the Redis password using environment variables:
```bash
export REDIS_PASSWORD=your-secure-password
```

## Data Model

### Redis Key Structure

Sessions are stored with the following key pattern:
```
ces:session:{sessionId}
```

Example:
```
ces:session:550e8400-e29b-41d4-a716-446655440000
```

### Session Data Structure

Each session is stored as JSON:
```json
{
  "sessionId": {
    "value": "550e8400-e29b-41d4-a716-446655440000"
  },
  "status": "CONNECTED",
  "createdAt": "2025-10-06T12:00:00Z",
  "lastHeartbeatAt": "2025-10-06T12:29:00Z",
  "disconnectedAt": null
}
```

### Time-to-Live (TTL)

Sessions have a default TTL of 24 hours, automatically refreshed on heartbeat updates. This prevents Redis from accumulating stale sessions.

## Monitoring

### Redis Metrics

Monitor the following Redis metrics:
- Connected clients
- Memory usage
- Key count (session count)
- Commands per second
- Hit/miss ratio

### Application Metrics

The application exposes metrics via Spring Boot Actuator:
```
GET /actuator/metrics/ces.sessions.active
GET /actuator/metrics/ces.sessions.registered
```

## Broadcasting Support

The Redis implementation supports broadcasting to all active sessions by:

1. Scanning all session keys matching the pattern `ces:session:*`
2. Retrieving each session to check if it's active
3. Sending messages to each active session

**Note**: For very high session counts (>10,000), consider using Redis Pub/Sub for broadcasting to improve performance.

## Troubleshooting

### Connection Issues

**Problem**: Application can't connect to Redis

**Solution**:
1. Check Redis is running: `redis-cli ping`
2. Verify connection settings in configuration
3. Check firewall rules
4. Verify password (if configured)

### Session Not Found

**Problem**: Session exists but isn't found

**Solution**:
1. Check session hasn't expired (24-hour TTL)
2. Verify correct profile is active
3. Check Redis key pattern matches

### Performance Issues

**Problem**: Slow session operations

**Solution**:
1. Increase connection pool size
2. Reduce timeout values if network is fast
3. Consider Redis cluster for horizontal scaling
4. Monitor Redis memory usage

## Migration from In-Memory to Redis

When deploying to production for the first time:

1. Ensure Redis is configured and accessible
2. Set `ces.session.storage=redis` in production profile
3. Deploy application
4. Existing sessions will be lost during migration (expected)
5. Clients will automatically reconnect and re-register

## Security Considerations

1. **Always use password authentication** in production
2. **Use TLS/SSL** for Redis connections in production
3. **Restrict network access** to Redis using security groups/firewall
4. **Rotate passwords regularly**
5. **Monitor for unauthorized access attempts**

## Testing

### Unit Tests

Redis functionality is tested with mocked RedisTemplate:
```bash
mvn test -Dtest=RedisSessionRegistryTest
```

### Integration Tests

To run integration tests with real Redis:
```bash
# Start Redis
docker run -d --name redis-test -p 6379:6379 redis:7-alpine

# Run tests
mvn verify -P integration-tests

# Cleanup
docker stop redis-test && docker rm redis-test
```

## References

- [Spring Data Redis Documentation](https://spring.io/projects/spring-data-redis)
- [Redis Documentation](https://redis.io/docs/)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
