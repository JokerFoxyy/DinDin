CREATE TABLE recurring_transactions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users (id),
    account_id   UUID NOT NULL REFERENCES accounts (id),
    category_id  UUID NOT NULL REFERENCES categories (id),
    description  VARCHAR(200)   NOT NULL,
    amount       NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    type         VARCHAR(20)    NOT NULL CHECK (type IN ('EXPENSE', 'INCOME')),
    day_of_month INTEGER        NOT NULL CHECK (day_of_month BETWEEN 1 AND 31),
    active       BOOLEAN        NOT NULL DEFAULT TRUE,
    end_date     DATE           NULL,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_recurring_user ON recurring_transactions (user_id);

-- liga a transação materializada ao fixo de origem + flag de pagamento
ALTER TABLE transactions
    ADD COLUMN recurring_id UUID NULL REFERENCES recurring_transactions (id),
    ADD COLUMN paid BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_transactions_recurring ON transactions (recurring_id);
