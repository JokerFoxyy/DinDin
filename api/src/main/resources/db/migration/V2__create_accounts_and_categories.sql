CREATE TABLE accounts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id),
    name        VARCHAR(100) NOT NULL,
    type        VARCHAR(20)  NOT NULL CHECK (type IN ('CHECKING', 'CREDIT_CARD', 'CASH')),
    closing_day INTEGER      NULL CHECK (closing_day BETWEEN 1 AND 31),
    due_day     INTEGER      NULL CHECK (due_day BETWEEN 1 AND 31),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_user ON accounts (user_id);

CREATE TABLE categories (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users (id),
    name       VARCHAR(60) NOT NULL,
    icon       VARCHAR(16) NULL,
    color      VARCHAR(7)  NULL,
    kind       VARCHAR(10) NOT NULL CHECK (kind IN ('EXPENSE', 'INCOME')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_categories_user_name_kind UNIQUE (user_id, name, kind)
);

CREATE INDEX idx_categories_user ON categories (user_id);
