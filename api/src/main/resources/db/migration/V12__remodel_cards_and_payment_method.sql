-- Sessão #25: remodelagem contas & cartões.
-- Decisão do usuário: recomeçar os dados transacionais (app pré-produção) —
-- por isso o wipe abaixo em vez de migração de dados.

-- 1. Wipe transacional (ordem respeita FKs)
DELETE FROM transactions;
DELETE FROM card_invoices;
DELETE FROM recurring_transactions;
DELETE FROM accounts WHERE type = 'CREDIT_CARD';

-- 2. Contas ficam só com CHECKING/CASH; dias de fatura saem da conta
ALTER TABLE accounts DROP CONSTRAINT accounts_type_check;
ALTER TABLE accounts ADD CONSTRAINT accounts_type_check CHECK (type IN ('CHECKING', 'CASH'));
ALTER TABLE accounts DROP COLUMN closing_day;
ALTER TABLE accounts DROP COLUMN due_day;

-- 3. Cartões de crédito: instrumento de pagamento vinculado a uma conta
CREATE TABLE cards (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id),
    account_id  UUID NOT NULL REFERENCES accounts (id),
    name        VARCHAR(100) NOT NULL,
    closing_day INTEGER NOT NULL CHECK (closing_day BETWEEN 1 AND 31),
    due_day     INTEGER NOT NULL CHECK (due_day BETWEEN 1 AND 31),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_cards_user ON cards (user_id);
CREATE INDEX idx_cards_account ON cards (account_id);

-- 4. Faturas passam a pertencer ao cartão (tabela vazia após o wipe)
ALTER TABLE card_invoices DROP CONSTRAINT uq_card_invoices_account_month;
ALTER TABLE card_invoices DROP COLUMN account_id;
ALTER TABLE card_invoices ADD COLUMN card_id UUID NOT NULL REFERENCES cards (id);
ALTER TABLE card_invoices ADD CONSTRAINT uq_card_invoices_card_month UNIQUE (card_id, month);

-- 5. Transação aponta pra exatamente UM de conta (débito/dinheiro) ou cartão (crédito)
ALTER TABLE transactions ALTER COLUMN account_id DROP NOT NULL;
ALTER TABLE transactions ADD COLUMN card_id UUID NULL REFERENCES cards (id);
ALTER TABLE transactions ADD CONSTRAINT chk_transactions_account_xor_card
    CHECK ((account_id IS NOT NULL AND card_id IS NULL) OR (account_id IS NULL AND card_id IS NOT NULL));
CREATE INDEX idx_transactions_card ON transactions (card_id);

-- 6. Novo tipo reservado: pagamento de fatura (debita a conta vinculada; excluído dos gastos)
ALTER TABLE transactions DROP CONSTRAINT transactions_type_check;
ALTER TABLE transactions ADD CONSTRAINT transactions_type_check
    CHECK (type IN ('EXPENSE', 'INCOME', 'INVOICE_ADJUSTMENT', 'INVOICE_PAYMENT'));
