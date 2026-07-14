# SDD — Sessão #9: Fechamento de Fatura

> **Data:** 2026-07-12 · **Pré-req:** #6 ✅ · **Branch:** `feature/fechamento-fatura` · **Plano-mestre:** `docs/PLANO-SDD.md`

## Objetivo

Fechar a fatura do cartão: comparar o total lançado com o valor real informado e, se houver diferença (lançamentos esquecidos), criar automaticamente um `INVOICE_ADJUSTMENT` que zera a diferença. Ao detalhar os gastos depois, o ajuste diminui. Ciclo OPEN → CLOSED → PAID.

## Migration

**Nenhuma** — `card_invoices` (V3) já tem `declared_total` e `status` (OPEN/CLOSED/PAID); `INVOICE_ADJUSTMENT` já é um `TransactionType`.

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Total lançado | soma dos `amount` das transações da fatura **exceto** `INVOICE_ADJUSTMENT` | os ajustes não contam como lançamento real |
| Ajuste | `diff = declared − launched`; se `diff > 0` cria/atualiza um único `INVOICE_ADJUSTMENT` (amount = diff); se `diff ≤ 0` remove o ajuste | `amount > 0` no schema; over-lançamento não gera ajuste negativo |
| "Detalhar depois reduz o ajuste" | recalculado no `getDetail` quando a fatura está CLOSED e tem `declared_total` (reconciliação idempotente) | implementa a regra da spec sem acoplar `TransactionService` |
| Ajuste (transação) | conta = cartão, categoria = null, data = `closing_date`, type = INVOICE_ADJUSTMENT, descrição "Ajuste de fatura", `invoice_id` da fatura | criado direto via repositório (não passa pela validação que bloqueia o tipo) |
| Transições | OPEN→CLOSED (`close`), CLOSED→PAID (`pay`), CLOSED→OPEN (`reopen`); transição inválida → 400 | ciclo de vida controlado |
| Escopo | fatura pertence à conta que pertence ao usuário → valida via `accountRepository.findByIdAndUserId`; alheia → 404 | mesma regra dos outros recursos |
| Endpoints | `GET /v1/invoices?month=`, `GET /v1/invoices/{id}`, `POST /{id}/close`, `POST /{id}/pay`, `POST /{id}/reopen` | REST |
| Isolamento do #8 | não altera `TransactionService`/`Transaction` (só adiciona finders no repo) — reduz conflito com o PR #15 (fixos) ainda não mergeado | merge limpo |

## Tasks

- **TASK-1 — Domínio**: métodos `close/pay/reopen` em `CardInvoice`; finders em `CardInvoiceRepository`/`TransactionRepository`.
- **TASK-2 — InvoiceService**: list, detail (com recompute do ajuste), close (com ajuste), pay, reopen; DTOs (summary/detail).
- **TASK-3 — InvoiceController** `/v1/invoices`.
- **TASK-4 — Testes backend**: unit (ajuste: cria/atualiza/remove, detalhar reduz, transições inválidas) + integração (fluxo close→ajuste→detalhar→pay + 400/404/401); JaCoCo ≥90%.
- **TASK-5 — Frontend**: item "Faturas" na sidebar + tela (month-picker, cartões com lançado × declarado × status, ações fechar/pagar/reabrir, detalhe com transações e ajuste); service + specs.
- **TASK-6 — Verificação end-to-end**: `mvnw verify` + `npm run test:ci` verdes; browser (lançar no cartão → fechar com valor maior → ajuste aparece → detalhar reduz → pagar); PR com CI verde (entregue ao usuário).

## Status

- [x] TASK-1 — Domínio
- [x] TASK-2 — InvoiceService
- [x] TASK-3 — InvoiceController
- [x] TASK-4 — Testes backend
- [x] TASK-5 — Frontend
- [x] TASK-6 — Verificação end-to-end

**Resultado da verificação (2026-07-13):**
- API: `mvnw verify` — **119 testes**, JaCoCo ≥90% (InvoiceService: ajuste cria/atualiza/remove, detalhar reduz, reconciliação no list, transições inválidas; InvoiceFlow integração)
- Web: `npm run test:ci` — **89 testes**, cobertura 96,4/87,1/90,1/96,2; `build:prod` OK
- Browser (stack completo): compra R$100 no cartão → tela Faturas mostra fatura Aberta (Lançado R$100) → fechar declarando R$150 → **ajuste R$50** e status Fechada → lançar os R$50 esquecidos → ao recarregar, **ajuste reconciliado para 0** (Lançado = Declarado, transação de ajuste removida) → **Pagar** → status Paga
- **Reconciliação também no `list()`** (não só no `getDetail`) — senão a listagem mostraria ajuste desatualizado após detalhar
- Isolado do #8: só adiciona finders/métodos (não altera `TransactionService`); sem migration
- Novo item na sidebar: **💳 Faturas** (`/faturas`)
