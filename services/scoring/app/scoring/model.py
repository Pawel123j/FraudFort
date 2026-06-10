"""Scoring models.

`ScoringModel` is the seam that stage 7 swaps the real MLflow/XGBoost model into.
`HeuristicModel` is a transparent placeholder: deterministic, fast (<1ms), and
explainable, so the rest of the platform (decision engine, SOC panel, monitoring)
can be built and demoed against a stable contract before the ML model is wired in.
"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import timezone
from typing import Protocol, runtime_checkable

from app.schemas import ScoreRequest


@dataclass(frozen=True)
class ScoreOutcome:
    """A model's raw verdict, before the service wraps it with metadata."""

    fraud_score: float
    reasons: list[str]


@runtime_checkable
class ScoringModel(Protocol):
    """Contract every scoring model must satisfy."""

    @property
    def version(self) -> str: ...

    def score(self, tx: ScoreRequest) -> ScoreOutcome: ...


# --- Heuristic placeholder ---------------------------------------------------

# High-risk merchant category codes -> human-readable label for explainability.
_HIGH_RISK_MCC: dict[str, str] = {
    "7995": "gambling",
    "6011": "atm_cash_withdrawal",
    "4829": "money_transfer",
    "6051": "quasi_cash_or_crypto",
}

# Illustrative high-risk jurisdictions (sanctions / elevated card-fraud rates).
# In production this would come from a maintained reference list, not a constant.
_HIGH_RISK_COUNTRIES: frozenset[str] = frozenset({"RU", "IR", "KP", "NG", "VE"})

_AMOUNT_ELEVATED = 1_000.0
_AMOUNT_HIGH = 5_000.0
_OFF_HOURS_UTC = range(0, 6)  # 00:00–05:59 UTC


class HeuristicModel:
    """Weighted-signal scorer. Each triggered signal adds risk; total is clamped
    to [0, 1]. Weights are intentionally legible rather than tuned."""

    version = "heuristic-0.1.0"

    def score(self, tx: ScoreRequest) -> ScoreOutcome:
        risk = 0.0
        reasons: list[str] = []

        if tx.amount >= _AMOUNT_HIGH:
            risk += 0.40
            reasons.append("high_amount")
        elif tx.amount >= _AMOUNT_ELEVATED:
            risk += 0.20
            reasons.append("elevated_amount")

        hour_utc = tx.occurred_at.astimezone(timezone.utc).hour
        if hour_utc in _OFF_HOURS_UTC:
            risk += 0.20
            reasons.append("off_hours_utc")

        label = _HIGH_RISK_MCC.get(tx.merchant_category)
        if label is not None:
            risk += 0.30
            reasons.append(f"high_risk_mcc:{label}")

        if tx.country in _HIGH_RISK_COUNTRIES:
            risk += 0.25
            reasons.append("high_risk_country")

        if not tx.device_id:
            risk += 0.15
            reasons.append("missing_device")

        fraud_score = round(min(1.0, risk), 4)
        return ScoreOutcome(fraud_score=fraud_score, reasons=reasons)
