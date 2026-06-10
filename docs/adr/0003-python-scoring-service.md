# ADR 0003 — Python scoring service with FastAPI and uv

**Status:** Accepted
**Date:** 2026-06-10

## Context

An existing fraud detection model (scikit-learn / XGBoost, with SHAP and Optuna
tuning, tracked in MLflow) needs to be exposed as a low-latency scoring endpoint
the orchestrator can call synchronously per transaction. Target latency: **< 50 ms p95**.

The model is Python-native; rewriting it in Java (ONNX / DJL) would add risk and
slow iteration on the ML side. A thin Python service is the path of least resistance.

## Decision

- **Python 3.12**
- **FastAPI** (Starlette + Pydantic v2) — single `/score` endpoint accepting a
  Transaction, returning `{ fraud_score, model_version, feature_contributions }`
- **uv** for dependency management and virtualenv handling — `pyproject.toml` is
  the source of truth, with `uv.lock` committed for reproducible builds
- **MLflow client** (stage 7) to pull the model from the registry at startup; for
  stages 2 and 3 a placeholder model is used
- Base package: `app/`

## Consequences

**Positive**
- FastAPI's ASGI + Pydantic v2 path is fast enough for the < 50 ms p95 target on a
  preloaded model
- `uv` installs are 10–100× faster than pip/poetry — meaningful for CI and Docker
  layer caching
- Existing scikit-learn / XGBoost model integrates without translation
- Pydantic v2 schemas auto-generate OpenAPI, which feeds back into `shared/contracts/`

**Negative**
- Python GIL caps single-process throughput — for higher load, scale horizontally
  with uvicorn workers (sized to the model's memory footprint)
- `uv` is a newer tool, less familiar in conservative banking environments —
  mitigated by the fact that `pyproject.toml` itself is standard

## Alternatives considered

- **Flask + Gunicorn.** Older sync stack, slower per request, less ergonomic schemas.
- **Java-side model via ONNX / DJL.** Would let the orchestrator do scoring in-process
  but adds a model-conversion step and decouples from the Python training tooling.
- **NVIDIA Triton Inference Server.** Overkill at this scale and adds operational
  weight that doesn't pay off until much higher QPS.
- **poetry instead of uv.** Mature and widely understood, but materially slower; the
  speed difference shows up in every Dockerfile rebuild.
