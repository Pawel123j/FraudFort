-- V1 baseline: transactions table.
-- Status / fraud_score / decision_reason are added by the decision engine in stage 3.

CREATE TABLE transactions (
    id                  UUID            PRIMARY KEY,
    card_id             VARCHAR(64)     NOT NULL,
    customer_id         VARCHAR(64)     NOT NULL,
    amount              NUMERIC(19, 4)  NOT NULL,
    currency            VARCHAR(3)      NOT NULL,
    merchant_category   VARCHAR(4)      NOT NULL,
    country             VARCHAR(2)      NOT NULL,
    device_id           VARCHAR(64),
    occurred_at         TIMESTAMPTZ     NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Hot paths planned for later stages:
--   stage 6: per-card and per-customer velocity windows -> indexes on (card_id, occurred_at)
--   stage 5: SOC analyst queue by occurred_at DESC
-- For now we index the obvious lookup columns; tighten in stage 6.
CREATE INDEX idx_transactions_card_id      ON transactions (card_id);
CREATE INDEX idx_transactions_customer_id  ON transactions (customer_id);
CREATE INDEX idx_transactions_occurred_at  ON transactions (occurred_at DESC);
