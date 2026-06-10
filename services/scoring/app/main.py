"""FastAPI application exposing the fraud scoring model.

Endpoints:
    GET  /health  -> liveness + which model is loaded
    POST /score   -> fraud verdict for one transaction
"""

from __future__ import annotations

from datetime import datetime, timezone

from fastapi import FastAPI

from app import __version__
from app.schemas import ScoreRequest, ScoreResponse
from app.scoring.model import HeuristicModel, ScoringModel

app = FastAPI(
    title="FraudFort Scoring Service",
    version=__version__,
    summary="Real-time transaction fraud scoring.",
)

# The active model. Swapped for the MLflow-backed model in stage 7; the rest of
# the app depends only on the ScoringModel protocol, not the concrete class.
model: ScoringModel = HeuristicModel()


@app.get("/health", tags=["ops"])
def health() -> dict[str, str]:
    return {"status": "UP", "model_version": model.version}


@app.post("/score", response_model=ScoreResponse, tags=["scoring"])
def score(request: ScoreRequest) -> ScoreResponse:
    outcome = model.score(request)
    return ScoreResponse(
        fraud_score=outcome.fraud_score,
        model_version=model.version,
        reasons=outcome.reasons,
        scored_at=datetime.now(timezone.utc),
    )
