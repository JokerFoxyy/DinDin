CREATE TABLE goals (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users (id),
    name          VARCHAR(120)   NOT NULL,
    target_amount NUMERIC(14, 2) NOT NULL CHECK (target_amount > 0),
    target_date   DATE           NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_goals_user ON goals (user_id);

CREATE TABLE goal_contributions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id    UUID NOT NULL REFERENCES goals (id) ON DELETE CASCADE,
    month      DATE           NOT NULL, -- primeiro dia do mês
    amount     NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_goal_contributions_goal ON goal_contributions (goal_id);
