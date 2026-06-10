"""Public request/response contract for the scoring service.

These Pydantic models ARE the API contract the Spring orchestrator codes against
(see services/orchestrator ScoreRequest/ScoreResponse DTOs). Field names are
snake_case on the wire; the orchestrator maps them with @JsonNaming.
"""

from __future__ import annotations

from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class ScoreRequest(BaseModel):
    """A single transaction submitted for fraud scoring."""

    model_config = ConfigDict(extra="forbid")

    transaction_id: UUID | None = Field(
        default=None,
        description="Orchestrator-assigned id, echoed in logs for correlation.",
    )
    card_id: str = Field(min_length=1, max_length=64)
    customer_id: str = Field(min_length=1, max_length=64)
    amount: float = Field(ge=0, description="Transaction amount in the given currency.")
    currency: str = Field(pattern=r"^[A-Z]{3}$", description="ISO 4217.")
    merchant_category: str = Field(pattern=r"^[0-9]{4}$", description="4-digit MCC.")
    country: str = Field(pattern=r"^[A-Z]{2}$", description="ISO 3166-1 alpha-2.")
    device_id: str | None = Field(default=None, max_length=64)
    occurred_at: datetime


class ScoreResponse(BaseModel):
    """The model's verdict for a transaction."""

    fraud_score: float = Field(
        ge=0.0, le=1.0, description="Calibrated fraud probability in [0, 1]."
    )
    model_version: str = Field(description="Identifies the scoring model and its weights.")
    reasons: list[str] = Field(
        default_factory=list,
        description="Human-readable signals that drove the score (explainability).",
    )
    scored_at: datetime
