# SDD — Sessão #25: Remodelagem Contas & Cartões (método de pagamento)

> **Data:** 2026-07-22 · **Branch:** `feature/remodelagem-contas-cartoes` · **Plano-mestre:** `docs/PLANO-SDD.md`
> **Fase 4 — executada imediatamente, antes de #24/#22:** o rearranjo de domínio precisa acontecer antes das próximas features (pedido do usuário).

## Motivação

Hoje "cartão de crédito" é um **tipo de conta**, irmão da conta corrente — o que mistura dois conceitos ("onde o dinheiro vive" × "instrumento de pagamento") e deixa a listagem de transações confusa. Além disso, o importador salvava transações direto no repositório, **pulando a regra de vínculo de fatura** — por isso a aba Faturas ficava zerada após importar (bug real encontrado nesta investigação; será corrigido aqui dentro da remodelagem, decisão do usuário de pular o bugfix isolado).

## Decisões (confirmadas com o usuário em 2026-07-22)

| Decisão | Escolha | Consequência |
|---|---|---|
| Modelo | **Entidade `Card` separada**, sempre vinculada a uma conta (`account_id NOT NULL`) | `Account` fica só com `CHECKING`/`CASH` (tipo `CREDIT_CARD` deixa de existir; `closing_day`/`due_day` saem de `accounts` e vão pro cartão). |
| Saldo | Compra no crédito **só afeta a conta quando a fatura é paga** | Compra no crédito vira transação do **cartão** (não da conta). Pagar a fatura cria uma transação especial `INVOICE_PAYMENT` na conta vinculada (caixa); os gastos do mês continuam contando as compras (competência) e **excluem** o pagamento de fatura para não contar duas vezes. |
| Dados existentes | **Recomeçar** (app ainda não está em produção; dados atuais eram testes de import) | Migration V12 apaga `transactions`, `card_invoices`, `recurring_transactions` e as contas `CREDIT_CARD`. Contas CHECKING/CASH, categorias, orçamentos, metas, investimentos e usuários são preservados. |

## Novo modelo

```
accounts (CHECKING | CASH)          cards (crédito)
   ▲  saldo = caixa                    ├─ account_id  → conta que paga a fatura
   │                                   ├─ closing_day / due_day
   │                                   ▼
   │                                card_invoices (por cartão × mês)
   │                                   ▲
   └── transactions ──────────────────┘
        exatamente UM de: account_id (débito/dinheiro) | card_id (crédito)
        método derivado (nunca digitado):
          card_id != null            → CRÉDITO
          account.type == CASH       → DINHEIRO
          senão                      → DÉBITO
        type: EXPENSE | INCOME | INVOICE_ADJUSTMENT | INVOICE_PAYMENT (novo, reservado)
```

- **Parcelamentos (`installments > 1`)**: passam a exigir cartão (parcelar débito não existe na vida real).
- **Fixos recorrentes**: continuam apontando **conta** nesta sessão (fixo no cartão fica de fora do escopo — anotado como melhoria futura).
- **Pagar fatura**: `POST /v1/invoices/{id}/pay` (body opcional `{accountId}`, default = conta vinculada do cartão) — cria `INVOICE_PAYMENT` do total na conta, marca a fatura `PAID`. `INVOICE_PAYMENT` é reservado (400 se tentado via POST normal), aparece na listagem, e é **excluído** de: gastos do dashboard, orçado×realizado, ajuste automático de fatura.
- **Import**: mapeamento da planilha pode apontar para **conta** ou **cartão** (criar cartão exige conta vinculada + dias de fechamento/vencimento); linhas mapeadas pra cartão passam pela regra de fatura — **corrige o bug da aba Faturas zerada**.
- **Export CSV/xlsx**: coluna "Conta" vira "Conta/Cartão" + nova coluna "Método".
- **LGPD**: export e delete de conta incluem `cards`.

## Tasks

- **TASK-1** — Migration **V12** (wipe transacional + `cards` + `card_invoices.card_id` + `transactions.card_id`/`account_id` nullable com CHECK xor + remove colunas de cartão de `accounts`) + entidade/CRUD `Card` (`/v1/cards`).
- **TASK-2** — Backend: `TransactionService` (xor conta/cartão, método derivado no response, installments só cartão), `CardInvoiceService` por cartão, endpoint de pagamento de fatura, exclusões de `INVOICE_PAYMENT` (dashboard/budget/export), `AccountType` sem `CREDIT_CARD`.
- **TASK-3** — Backend: importer roteando cartão→fatura; LGPD com cards.
- **TASK-4** — Testes backend (JaCoCo ≥90%).
- **TASK-5** — Frontend: painel Cartões em Configurações; form de transação com "Pagar com" (contas + cartões agrupados); badge de método na listagem; tela Faturas por cartão com ação Pagar; import com mapeamento pra cartão.
- **TASK-6** — Testes web (≥90/80/90/90) + verificação end-to-end no browser (criar cartão, compra crédito/débito/dinheiro, fatura gerada, pagar fatura debita a conta, import pra cartão gera fatura).
- **TASK-7** — Docs (CLAUDE.md, PLANO-SDD.md) + PR.

## Status

- [ ] TASK-1 · [ ] TASK-2 · [ ] TASK-3 · [ ] TASK-4 · [ ] TASK-5 · [ ] TASK-6 · [ ] TASK-7
