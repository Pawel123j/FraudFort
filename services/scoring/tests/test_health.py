from __future__ import annotations

from fastapi.testclient import TestClient


def test_health_reports_up_and_model_version(client: TestClient) -> None:
    response = client.get("/health")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "UP"
    assert body["model_version"] == "heuristic-0.1.0"
