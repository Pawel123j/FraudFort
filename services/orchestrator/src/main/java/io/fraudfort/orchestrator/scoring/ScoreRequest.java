package io.fraudfort.orchestrator.scoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.fraudfort.orchestrator.transaction.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payload sent to the scoring service. Field names are mapped explicitly to the
 * snake_case wire contract (see services/scoring/app/schemas.py). Explicit
 * {@code @JsonProperty} is used rather than a naming strategy so it is honored
 * regardless of the Jackson version backing the HTTP client.
 */
public record ScoreRequest(
        @JsonProperty("transaction_id") UUID transactionId,
        @JsonProperty("card_id") String cardId,
        @JsonProperty("customer_id") String customerId,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("merchant_category") String merchantCategory,
        @JsonProperty("country") String country,
        @JsonProperty("device_id") String deviceId,
        @JsonProperty("occurred_at") Instant occurredAt
) {
    public static ScoreRequest from(Transaction tx) {
        return new ScoreRequest(
                tx.getId(),
                tx.getCardId(),
                tx.getCustomerId(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getMerchantCategory(),
                tx.getCountry(),
                tx.getDeviceId(),
                tx.getOccurredAt()
        );
    }
}
