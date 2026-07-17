ALTER TABLE transactions
    ADD COLUMN installment_group_id UUID,
    ADD COLUMN installment_number   INTEGER,
    ADD COLUMN installment_count    INTEGER;

CREATE INDEX idx_transactions_installment_group ON transactions (installment_group_id);
