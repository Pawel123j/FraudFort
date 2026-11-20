-- V2: data ownership (FF-01). Every transaction belongs to the API client that submitted it
-- (the authenticated OAuth2 client / JWT subject). Clients can read only their own rows;
-- only the fraud-analyst role reads across clients.
--
-- Rows created before authentication existed have no known owner. They are assigned to a
-- sentinel that no real client can hold, so they stay visible to analysts only.
ALTER TABLE transactions ADD COLUMN client_id VARCHAR(255);
UPDATE transactions SET client_id = 'legacy:unowned' WHERE client_id IS NULL;
ALTER TABLE transactions ALTER COLUMN client_id SET NOT NULL;
CREATE INDEX idx_transactions_client_created ON transactions (client_id, created_at DESC);
