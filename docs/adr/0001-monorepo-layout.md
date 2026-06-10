# ADR 0001 — Monorepo layout

**Status:** Accepted
**Date:** 2026-06-10

## Context

FraudFort is a multi-language system: a Java orchestrator, multiple Python services
(scoring, transaction generator, drift monitor), a TypeScript frontend, and shared
contracts (OpenAPI specs) used by both Java and Python. The orchestrator and scoring
service share a Transaction schema, and any change to it must be picked up by both.

For a solo portfolio project, polyrepo coordination overhead (cross-repo PRs, version
matrices, multiple CI pipelines) outweighs the isolation benefit.

## Decision

Single monorepo with a flat top-level grouping:

- `services/` — deployable units, each with its own Dockerfile, README and tests
- `infra/` — Prometheus, Grafana, Kubernetes manifests
- `shared/contracts/` — OpenAPI specs as single source of truth
- `docs/` and `docs/adr/` — architecture documentation
- `ml/` — MLflow integration code (the model itself lives in MLflow)
- `docker-compose.yml` at repo root — visible entry point for local dev

Each service stays independently buildable and dockerizable. Cross-service code reuse
goes through `shared/contracts/`, not direct imports.

## Consequences

**Positive**
- Atomic cross-service changes (e.g. contract change + both consumers in one PR)
- Single Git history, single CI pipeline, single issue tracker
- Easy local "clone and run" experience for an interviewer

**Negative**
- Requires discipline to keep services from leaking into each other
- Build time grows with the repo (mitigated by per-service builds in CI)

## Alternatives considered

- **Polyrepo (one repo per service).** Rejected: too much coordination overhead for a
  solo project; contract drift becomes likely.
- **`apps/` + `packages/` (Nx/Turbo style).** Rejected: services don't yet share
  library code, so the extra structure is premature.
