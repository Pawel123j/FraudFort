package io.fraudfort.orchestrator.transaction.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateTransactionRequest(
        @NotBlank @Size(max = 64) String cardId,
        @NotBlank @Size(max = 64) String customerId,
        @NotNull @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be ISO 4217 (e.g. EUR)") String currency,
        @NotBlank @Pattern(regexp = "[0-9]{4}", message = "must be a 4-digit MCC") String merchantCategory,
        @NotBlank @Pattern(regexp = "[A-Z]{2}", message = "must be ISO 3166-1 alpha-2 (e.g. PL)") String country,
        @Size(max = 64) String deviceId,
        @NotNull Instant occurredAt
) {
}
