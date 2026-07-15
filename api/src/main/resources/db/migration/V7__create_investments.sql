CREATE TABLE investments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id),
    name        VARCHAR(120) NOT NULL,
    class       VARCHAR(20)  NOT NULL CHECK (class IN ('RESERVA', 'RENDA_FIXA', 'RENDA_VARIAVEL')),
    institution VARCHAR(120) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_investments_user ON investments (user_id);

CREATE TABLE investment_entries (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    investment_id UUID NOT NULL REFERENCES investments (id) ON DELETE CASCADE,
    date          DATE           NOT NULL,
    type          VARCHAR(20)    NOT NULL CHECK (type IN ('APORTE', 'RESGATE', 'ATUALIZACAO_SALDO')),
    amount        NUMERIC(14, 2) NOT NULL,
    balance_after NUMERIC(14, 2),
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT chk_investment_entries_balance_after CHECK (
        type <> 'ATUALIZACAO_SALDO' OR balance_after IS NOT NULL
    )
);

CREATE INDEX idx_investment_entries_investment_date ON investment_entries (investment_id, date);
