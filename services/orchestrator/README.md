# orchestrator

Spring Boot 4.0 service on Java 21 (virtual threads enabled). Owns the synchronous
transaction API, the streaming consumer (from stage 4), the decision engine, and
the call out to the Python scoring service.

## Local dev

Requires Docker (for Postgres + Redis from the root `docker-compose.yml`) and
JDK 21+ on PATH (the Gradle toolchain handles the rest).

```bash
# from repo root — bring up infra
docker compose up -d

# from services/orchestrator/
./gradlew bootRun

# health check
curl http://localhost:8080/actuator/health
```

## Tests

```bash
./gradlew test
```

Integration tests start a real PostgreSQL via Testcontainers, so Docker must be
running. No mocks of the database — see ADR 0002 for the rationale.

## Build

```bash
./gradlew build       # compile + test + jar
./gradlew bootJar     # just the runnable jar
```
