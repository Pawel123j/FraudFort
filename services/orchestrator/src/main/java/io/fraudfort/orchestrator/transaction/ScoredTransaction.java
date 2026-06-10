package io.fraudfort.orchestrator.transaction;

import io.fraudfort.orchestrator.scoring.ScoreResult;

/**
 * A persisted transaction paired with the score computed at intake. The score is
 * not stored in stage 2 — it is surfaced on the create response and becomes a
 * persisted decision (status + reason) in stage 3.
 */
public record ScoredTransaction(Transaction transaction, ScoreResult score) {
}
