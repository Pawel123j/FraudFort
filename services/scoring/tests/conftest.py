from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture
def client() -> TestClient:
    return TestClient(app)


@pytest.fixture
def low_risk_payload() -> dict:
    """A daytime, modest, card-present grocery purchase -> should score ~0."""
    return {
        "card_id": "card-1",
        "customer_id": "cust-1",
        "amount": 42.0,
        "currency": "EUR",
        "merchant_category": "5411",  # grocery stores
        "country": "PL",
        "device_id": "dev-1",
        "occurred_at": "2026-06-10T12:00:00Z",
    }


@pytest.fixture
def high_risk_payload() -> dict:
    """Large, off-hours, gambling, no device -> should score high."""
    return {
        "card_id": "card-9",
        "customer_id": "cust-9",
        "amount": 7500.0,
        "currency": "EUR",
        "merchant_category": "7995",  # gambling
        "country": "PL",
        "device_id": None,
        "occurred_at": "2026-06-10T02:30:00Z",
    }
