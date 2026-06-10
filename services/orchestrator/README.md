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

Integration tests start a real PostgreSQL via Testcontainers, so **Docker must be
running** before you run them. No mocks of the database — see ADR 0002 for the rationale.

## Troubleshooting

**`PKIX path building failed` / `unable to find valid certification path` on dependency
download.** You're behind a TLS-intercepting proxy (common in banking — Zscaler, etc.).
The proxy's CA is trusted by the OS but not by the JVM's bundled truststore. Point the
JVM at the OS trust store, e.g. on Windows:

```bash
./gradlew test -Djavax.net.ssl.trustStoreType=Windows-ROOT
```

or set it persistently in `~/.gradle/gradle.properties`:

```properties
systemProp.javax.net.ssl.trustStoreType=Windows-ROOT
```

**First build is slow.** Gradle downloads its distribution, the Java 21 toolchain (via the
Foojay resolver), and all dependencies on the first run. Subsequent runs are cached.

## Build

```bash
./gradlew build       # compile + test + jar
./gradlew bootJar     # just the runnable jar
```
