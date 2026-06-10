package io.fraudfort.orchestrator.transaction.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fraudfort.orchestrator.scoring.ScoreResult;
import io.fraudfort.orchestrator.transaction.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * API view of a transaction. The scoring fields are populated on the create
 * response (POST) and omitted when null — reads (GET) return them empty until
 * the score is persisted with the decision record in stage 3.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionResponse(
        UUID id,
        String cardId,
        String customerId,
        BigDecimal amount,
        String currency,
        String merchantCategory,
        String country,
        String deviceId,
        Instant occurredAt,
        Instant createdAt,
        Double fraudScore,
        String scoringStatus,
        List<String> scoreReasons
) {
    public static TransactionResponse from(Transaction tx) {
        return from(tx, null);
    }

    public static TransactionResponse from(Transaction tx, ScoreResult score) {
        return new TransactionResponse(
                tx.getId(),
                tx.getCardId(),
                tx.getCustomerId(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getMerchantCategory(),
                tx.getCountry(),
                tx.getDeviceId(),
                tx.getOccurredAt(),
                tx.getCreatedAt(),
                score == null ? null : score.fraudScore(),
                score == null ? null : score.status(),
                score == null ? null : score.reasons()
        );
    }
}
