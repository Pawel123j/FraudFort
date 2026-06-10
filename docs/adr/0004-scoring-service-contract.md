# ADR 0004 — Scoring service: separate FastAPI service, called synchronously, with graceful fallback

Status: Accepted · Date: 2026-06-10 · Stage: 2

## Context

Fraud scoring is an ML concern: the model (and the team's prior work) lives in the
Python ecosystem — scikit-learn / XGBoost, SHAP, MLflow. The orchestrator is a
Spring Boot service. We need the orchestrator to obtain a fraud score per
transaction at intake, while keeping the ML model where it belongs and swappable.

## Decision

- **Separate Python + FastAPI service** (`services/scoring`) exposing `POST /score`
  and `GET /health`. It owns the model; the orchestrator owns workflow/storage.
- **Stable wire contract** in snake_case (`card_id`, `fraud_score`, …). The
  orchestrator maps it with explicit `@JsonProperty` on its DTOs — not a Jackson
  naming strategy, because Spring Boot 4.0 defaults to Jackson 3 and a Jackson 2
  `@JsonNaming` annotation is silently ignored.
- **Model behind a `ScoringModel` seam.** Stage 2 ships a transparent, deterministic
  **heuristic** (weighted signals: amount, off-hours, risky MCC/country, missing
  device) that returns a score *and* human-readable `reasons`. Stage 7 swaps in the
  MLflow-backed model by implementing the same protocol — no contract change.
- **Synchronous call** from the orchestrator via Spring `RestClient` (JDK
  `HttpClient` factory, tight connect/read timeouts), scored **outside** the DB
  transaction so the HTTP call never holds a pooled connection.
- **Graceful degradation.** `ScoringClient` never throws: timeouts/errors yield
  `ScoreResult.unavailable()` (`scoringStatus = UNAVAILABLE`). Transaction intake is
  not coupled to scoring availability; the decision engine (stage 3) treats a
  missing score as its own case.
- **Score not persisted in stage 2.** It is surfaced on the create response only;
  persistence lands with the decision record (status + reason) in stage 3.

## Alternatives considered

- **Embed the model in the JVM (PMML/ONNX).** Rejected: forces the ML model into a
  foreign runtime, loses the Python tooling (SHAP, MLflow), and couples deploys.
- **Async/event-driven scoring (Kafka).** Deferred: streaming arrives in stage 4 for
  the ingestion path. The synchronous REST call keeps stage 2 simple and gives the
  create endpoint an immediate score.
- **Reactive `WebClient`.** Unnecessary: the call is a single blocking hop and the
  app runs on virtual threads, so a synchronous `RestClient` is simpler.

## Consequences

- A second runtime to build/deploy (Dockerized, in compose). Justified by clean
  separation of ML and orchestration.
- The orchestrator must tolerate scoring downtime — designed in via the fallback.
- Contract drift between the two services is a risk; mitigated by the explicit,
  documented snake_case contract and tests on both sides (pytest + `ScoringClientTest`).
