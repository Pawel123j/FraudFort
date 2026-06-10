# FraudFort — Scoring Service

Real-time fraud scoring over HTTP. Built with **FastAPI** and managed with **uv**.

Stage 2 ships a transparent **heuristic** model behind a stable `/score` contract.
The real MLflow/XGBoost model is plugged in at **stage 7** by implementing the
`ScoringModel` protocol — no API change for callers.

## Contract

### `POST /score`

Request:

```json
{
  "transaction_id": "0d2c…",        // optional, for log correlation
  "card_id": "card-9",
  "customer_id": "cust-9",
  "amount": 7500.0,
  "currency": "EUR",                  // ISO 4217
  "merchant_category": "7995",        // 4-digit MCC
  "country": "PL",                    // ISO 3166-1 alpha-2
  "device_id": null,
  "occurred_at": "2026-06-10T02:30:00Z"
}
```

Response:

```json
{
  "fraud_score": 1.0,                 // calibrated probability in [0, 1]
  "model_version": "heuristic-0.1.0",
  "reasons": ["high_amount", "off_hours_utc", "high_risk_mcc:gambling", "missing_device"],
  "scored_at": "2026-06-10T19:00:00Z"
}
```

Unknown fields and malformed values return `422` (the request model is `extra="forbid"`).

### `GET /health`

```json
{ "status": "UP", "model_version": "heuristic-0.1.0" }
```

## Heuristic model

Each triggered signal adds risk; the total is clamped to `[0, 1]`. Weights are
deliberately legible (not tuned) so the decision is explainable:

| Signal | Weight | Reason code |
|---|---|---|
| amount ≥ 5000 | 0.40 | `high_amount` |
| amount ≥ 1000 (and < 5000) | 0.20 | `elevated_amount` |
| 00:00–05:59 UTC | 0.20 | `off_hours_utc` |
| high-risk MCC (gambling, ATM, money transfer, quasi-cash) | 0.30 | `high_risk_mcc:<label>` |
| high-risk country | 0.25 | `high_risk_country` |
| no device id | 0.15 | `missing_device` |

## Run locally

```bash
cd services/scoring
uv sync                 # create venv + install deps (uv installs Python 3.12 if needed)
uv run uvicorn app.main:app --reload --port 8000
# -> http://localhost:8000/docs  (interactive OpenAPI)
```

If you are behind a TLS-intercepting proxy and uv fails to download, use the OS
trust store: `uv sync --native-tls` (or set `UV_NATIVE_TLS=1`).

## Test

```bash
uv run pytest
```

## Docker

```bash
docker build -t fraudfort/scoring:dev .
docker run --rm -p 8000:8000 fraudfort/scoring:dev
```

Or via the root `docker-compose.yml` (service `scoring`).
