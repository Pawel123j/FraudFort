package io.fraudfort.orchestrator.transaction.api;

import io.fraudfort.orchestrator.transaction.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
        Instant createdAt
) {
    public static TransactionResponse from(Transaction tx) {
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
                tx.getCreatedAt()
        );
    }
}
