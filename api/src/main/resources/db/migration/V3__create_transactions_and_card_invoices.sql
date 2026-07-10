CREATE TABLE card_invoices (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id     UUID NOT NULL REFERENCES accounts (id),
    month          DATE NOT NULL, -- primeiro dia do mês da fatura
    closing_date   DATE NOT NULL,
    due_date       DATE NOT NULL,
    declared_total NUMERIC(14, 2) NULL,
    status         VARCHAR(10) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED', 'PAID')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_card_invoices_account_month UNIQUE (account_id, month)
);

CREATE TABLE transactions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id),
    account_id  UUID NOT NULL REFERENCES accounts (id),
    category_id UUID NULL REFERENCES categories (id),
    invoice_id  UUID NULL REFERENCES card_invoices (id),
    description VARCHAR(200)   NOT NULL,
    amount      NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    date        DATE           NOT NULL,
    type        VARCHAR(20)    NOT NULL CHECK (type IN ('EXPENSE', 'INCOME', 'INVOICE_ADJUSTMENT')),
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_user_date ON transactions (user_id, date);
CREATE INDEX idx_transactions_account ON transactions (account_id);
CREATE INDEX idx_transactions_invoice ON transactions (invoice_id);
