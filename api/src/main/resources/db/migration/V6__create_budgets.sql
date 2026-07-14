CREATE TABLE budgets (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id),
    category_id UUID NOT NULL REFERENCES categories (id),
    month       DATE           NOT NULL, -- primeiro dia do mês
    amount      NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_budgets_user_category_month UNIQUE (user_id, category_id, month)
);

CREATE INDEX idx_budgets_user_month ON budgets (user_id, month);
