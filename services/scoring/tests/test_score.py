from __future__ import annotations

from fastapi.testclient import TestClient


def test_low_risk_transaction_scores_zero(client: TestClient, low_risk_payload: dict) -> None:
    response = client.post("/score", json=low_risk_payload)

    assert response.status_code == 200
    body = response.json()
    assert body["fraud_score"] == 0.0
    assert body["reasons"] == []
    assert body["model_version"] == "heuristic-0.1.0"
    assert "scored_at" in body


def test_high_risk_transaction_scores_high_with_reasons(
    client: TestClient, high_risk_payload: dict
) -> None:
    response = client.post("/score", json=high_risk_payload)

    assert response.status_code == 200
    body = response.json()
    # 0.40 (high_amount) + 0.20 (off_hours) + 0.30 (gambling) + 0.15 (no device)
    # = 1.05, clamped to 1.0
    assert body["fraud_score"] == 1.0
    assert set(body["reasons"]) >= {
        "high_amount",
        "off_hours_utc",
        "high_risk_mcc:gambling",
        "missing_device",
    }


def test_elevated_amount_only_scores_partial(client: TestClient, low_risk_payload: dict) -> None:
    payload = {**low_risk_payload, "amount": 1500.0}

    body = client.post("/score", json=payload).json()

    assert body["fraud_score"] == 0.2
    assert body["reasons"] == ["elevated_amount"]


def test_high_risk_country_adds_risk(client: TestClient, low_risk_payload: dict) -> None:
    payload = {**low_risk_payload, "country": "RU"}

    body = client.post("/score", json=payload).json()

    assert body["fraud_score"] == 0.25
    assert body["reasons"] == ["high_risk_country"]


def test_invalid_currency_is_rejected(client: TestClient, low_risk_payload: dict) -> None:
    payload = {**low_risk_payload, "currency": "eur"}  # must be 3 uppercase letters

    assert client.post("/score", json=payload).status_code == 422


def test_unknown_field_is_rejected(client: TestClient, low_risk_payload: dict) -> None:
    payload = {**low_risk_payload, "ssn": "123-45-6789"}  # extra='forbid'

    assert client.post("/score", json=payload).status_code == 422
