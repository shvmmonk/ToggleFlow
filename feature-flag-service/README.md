# Feature Flag Service

A high-performance remote **Feature Flag & Percentage Rollout Service** built with Java 17, Spring Boot 3.x, PostgreSQL, and Valkey (Redis-compatible cache). Includes an interactive modern web dashboard, client JavaScript SDK, unit tests, and Zerops deployment configuration.

---

## Architecture Overview

```
                          ┌───────────────────────────┐
                          │  Client Application /     │
                          │  Frontend Dashboard (SPA) │
                          └─────────────┬─────────────┘
                                        │
                    GET /api/evaluate/{flagKey}?projectApiKey=...&userId=...
                                        │
                                        ▼
                          ┌───────────────────────────┐
                          │    Spring Boot Service    │
                          └──────┬─────────────┬──────┘
                                 │             │
                    1. Check     │             │ 2. Fallback
                       Cache     │             │    on Miss
                                 ▼             ▼
                        ┌─────────────┐   ┌─────────────┐
                        │   Valkey    │   │ PostgreSQL  │
                        │ (Redis 60s) │   │  Database   │
                        └─────────────┘   └─────────────┘
```

1. **Evaluation Flow**:
   - The application checks **Valkey / Redis** for `eval:{projectApiKey}:{flagKey}:{userId}`.
   - On **Cache Hit**: Returns cached boolean result immediately with `source = "CACHE"`.
   - On **Cache Miss**: Queries **PostgreSQL** for flag rules, computes the deterministic rollout result, saves to Valkey cache (TTL 60s), logs the evaluation asynchronously, and returns with `source = "DATABASE"`.

2. **Deterministic Rollout Logic**:
   ```java
   int bucket = Math.abs((userId + flagKey).hashCode()) % 100;
   boolean enabled = flag.isEnabled() && bucket < flag.getRolloutPercentage();
   ```
   Ensures that a specific user ID always receives the exact same evaluation result for a given feature flag.

---

## API Reference

### Projects
- `POST /api/projects` - Create a project and generate an API key.
- `GET /api/projects` - List all projects.
- `GET /api/projects/{id}` - Get project details.

### Feature Flags
- `POST /api/flags` - Create a feature flag under a project.
- `GET /api/flags?projectId={id}` - List all feature flags for a project (with evaluation count analytics).
- `GET /api/flags/{id}` - Get single flag details.
- `PUT /api/flags/{id}` - Update flag state, name, description, or rollout percentage (triggers Redis cache eviction).
- `DELETE /api/flags/{id}` - Delete flag (triggers Redis cache eviction).

### Evaluation
- `GET /api/evaluate/{flagKey}?projectApiKey={apiKey}&userId={userId}` - Core evaluation endpoint.

---

## JavaScript SDK Usage

```javascript
// Include src/main/resources/static/js/sdk.js
const sdk = new FeatureFlagSDK({
  baseUrl: 'http://localhost:8080',
  projectApiKey: 'ff_live_your_project_api_key'
});

const isDarkModeEnabled = await sdk.isFeatureEnabled('dark_mode', 'user_9872');
if (isDarkModeEnabled) {
  enableDarkMode();
}
```

---

## Running Locally

### Prerequisites
- JDK 17+
- Maven 3.8+

### Build & Run Tests
```powershell
mvn test
```

### Run Service
```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
Navigate to `http://localhost:8080` in your web browser to open the Admin Dashboard.

---

## Zerops Deployment (`zerops.yaml`)

The application includes a ready-to-deploy `zerops.yaml` configured for Zerops platform:
```yaml
zerops:
  - setup: api
    build:
      base: java@21
      buildCommands:
        - mvn clean package -DskipTests
      deployFiles: target/*.jar
    run:
      base: java@21
      ports:
        - port: 8080
          httpSupport: true
      envVariables:
        DB_HOST: ${db_hostname}
        DB_PORT: 5432
        DB_NAME: ${db_dbName}
        DB_USER: ${db_user}
        DB_PASS: ${db_password}
        REDIS_HOST: ${cache_hostname}
        REDIS_PORT: 6379
      start: java -jar target/*.jar
```
