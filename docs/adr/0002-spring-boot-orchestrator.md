# ADR 0002 — Spring Boot orchestrator on Java 21 with Gradle Kotlin DSL

**Status:** Accepted
**Date:** 2026-06-10

## Context

The orchestrator owns the synchronous and streaming transaction flow: REST API,
JPA persistence, Kafka consumer (stage 4), call to the Python scoring service,
JWT/OAuth2 auth, and the decision engine. It must integrate cleanly with the
banking-Java ecosystem (the audience this project is aimed at) and be defensible
in interviews.

## Decision

- **Spring Boot 3.5** on **Java 21 LTS**
- **Gradle** with **Kotlin DSL** (`build.gradle.kts`)
- **Flyway 10** for database migrations
- **Testcontainers** for integration tests (real Postgres, not mocks)
- Base package: `io.fraudfort.orchestrator`

Java 21's virtual threads are enabled for the web layer — orchestrator calls are I/O
heavy (scoring service, Postgres, Redis), which is exactly the workload virtual
threads are designed for.

## Consequences

**Positive**
- Industry-standard stack in banking — minimal explanation overhead in interviews
- Virtual threads simplify the I/O-heavy synchronous path without going reactive
- Gradle Kotlin DSL gives IDE-grade autocomplete in build files; incremental builds
  outpace Maven on warm caches
- Testcontainers tests catch real migration and SQL dialect issues, not the mock
  version of them

**Negative**
- First-time KTS build is slower than Groovy DSL
- Spring Boot startup cost (a few seconds) is higher than Quarkus/Micronaut native
- Testcontainers integration tests require Docker running locally — accepted, since
  Docker is a project-wide assumption already

## Alternatives considered

- **Quarkus / Micronaut.** Smaller share in banking; harder to justify when the
  audience is Spring-fluent.
- **Maven.** Slower builds, XML overhead, no real upside for a new project.
- **Java 17.** Loses virtual threads — meaningful for an I/O-bound orchestrator.
- **Mocking the DB in tests.** Rejected: mock/prod divergence is exactly the kind
  of bug fraud-detection code can't afford. Tests hit real Postgres via Testcontainers.
