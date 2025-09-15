# Hi-Doc API Service

Secure API layer for subscriber management, AI proxying (OpenAI/ChatGPT, Grok, Gemini), health data processing, analytics, and operations health.

## Quick start

- Java: 21+
- Build: Maven

Build and run local (unit tests only):

```
mvn -q -DskipTests package
java -jar target/hi-doc-api-service.jar
```

Docker build and run:

```
mvn -DskipTests package
docker build -t hidoc/api:local .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://postgres.tailce422e.ts.net:5432/hidoc \
  -e DB_USERNAME=postgres -e DB_PASSWORD=postgres \
  -e OPENAI_API_KEY=your_key \
  hidoc/api:local
```

## Configuration

Key properties (can be set via environment variables):

- Database
  - spring.datasource.url (DB_URL)
  - spring.datasource.username (DB_USERNAME)
  - spring.datasource.password (DB_PASSWORD)
- AI Providers (see application.yml for defaults)
  - ai.providers.openai.api-key (OPENAI_API_KEY)
  - ai.providers.openai.model (OPENAI_MODEL)
  - ai.providers.openai.base-url (OPENAI_BASE_URL)
  - ai.providers.grok.* (GROK_API_KEY, GROK_MODEL, GROK_BASE_URL)
  - ai.providers.gemini.* (GEMINI_API_KEY, GEMINI_MODEL, GEMINI_BASE_URL)
- Rate limiting
  - rate-limiting.monthly-limit (default 100)
- Caching (Redis) — disabled by default
  - cache.enabled=false|true
  - cache.redis.host, cache.redis.port, cache.redis.ttl-seconds

Profiles/config files:
- application.yml (default)
- application-docker.yml (container-friendly overrides)

## Authentication

All API endpoints (except health and docs) require OAuth JWT in Authorization header (Bearer). Tokens from Google or Microsoft are validated using their JWKS.

Example:
```
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

## OpenAPI/Swagger

Springdoc OpenAPI is enabled. After starting the app, navigate to:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Note: server servlet context-path is /api for the application routes, but Swagger UI is exposed at root for convenience.

## Key Endpoints

- Health
  - GET /api/health
  - GET /api/health/detailed
  - GET /api/health/ready
  - GET /api/health/live
- Subscribers
  - POST /api/subscribers
  - GET /api/subscribers/{userId}
  - GET /api/subscribers/by-email?email=
  - PUT /api/subscribers/{userId}/status?status=
- AI Proxy
  - POST /api/ai/openai/chat
  - POST /api/ai/grok/chat
  - POST /api/ai/gemini/chat
- Health Data
  - POST /api/health/process
  - GET /api/health/history
  - GET /api/health/trends
- Analytics
  - GET /api/analytics/summary
  - GET /api/analytics/trends

See Swagger UI for request/response models.

## Usage Examples

Create or update subscriber:
```
curl -X POST http://localhost:8080/api/subscribers \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{
    "userId": "user-1",
    "email": "user1@example.com",
    "oauthProvider": "GOOGLE",
    "subscriptionStatus": "ACTIVE"
  }'
```

Chat (OpenAI):
```
curl -X POST http://localhost:8080/api/ai/openai/chat \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"message":"hello"}'
```

Fetch analytics summary:
```
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/analytics/summary
```

## Rate Limiting

Monthly request limit per user (default 100). When exceeded, API returns HTTP 429 with a descriptive error body. Only metadata is stored for usage tracking; request/response bodies are not persisted.

## Health and Monitoring

Use /api/health endpoints for readiness/liveness and component status (database, AI services). Actuator health/info are also exposed at /actuator/*.

## Caching

Optional Redis caching for subscribers and usage stats. Disabled by default. Enable with `CACHE_ENABLED=true` and configure Redis host/port.

## Testing

- Unit and Web tests: `mvn test`
- End-to-end (requires Docker):
  - `ENABLE_E2E=true mvn -Dtest=IntegrationE2ETest test`
  - Uses Testcontainers PostgreSQL and verifies an end-to-end flow.

## Deployment

- Build a fat JAR: `mvn package`
- Run with Java 21 JRE: `java -jar target/hi-doc-api-service.jar`
- Docker: use provided Dockerfile; configure environment via variables shown above.

## License
Proprietary. All rights reserved.
