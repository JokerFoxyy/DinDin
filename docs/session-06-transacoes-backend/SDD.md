# SDD — Sessão #6: Transações (backend)

> **Data:** 2026-07-09 · **Pré-req:** #5 ✅ · **Branch:** `feature/transacoes-backend` · **Plano-mestre:** `docs/PLANO-SDD.md`

## Objetivo

CRUD de transações com a regra central do app: lançamento em cartão de crédito é vinculado automaticamente à fatura (`card_invoice`) do período correto conforme o `closing_day`. Filtros por mês/conta/categoria/tipo com paginação. Sessão só de backend (a tela vem na #7).

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Migration V3 | `card_invoices` + `transactions` | Fatura precisa existir agora (FK `invoice_id`); ciclo de vida completo fica para a #9 |
| `recurring_id` | **Fica para a V4** (ALTER TABLE na sessão #8) | A spec lista na V3, mas a tabela-alvo do FK só nasce na #8 — evitar coluna sem FK |
| Dinheiro | `NUMERIC(14,2)` + `BigDecimal`, `amount > 0` sempre | Sinal vem do `type`, nunca do valor |
| **Regra de vínculo** | Compra no dia `closing_day` **ou depois** → fatura do mês seguinte; antes → fatura do mês | Convenção de cartões BR |
| Fatura (mês M) | `closing_date` = dia `closing_day` de M (clampado ao fim do mês); `due_date` = dia `due_day` de M se `due_day > closing_day`, senão de M+1 | Vencimento sempre após o fechamento; fevereiro/dia 31 clampado |
| Get-or-create | Fatura única por `(account_id, month)` (UNIQUE); criada `OPEN` na primeira compra do período | Idempotente |
| Re-vínculo | Update de data/conta recalcula a fatura; mudar de cartão para conta comum zera `invoice_id` | Consistência |
| `INVOICE_ADJUSTMENT` | **Rejeitado na API** (400) — tipo reservado ao fechamento de fatura (#9) | Não deixar o usuário forjar ajustes |
| Coerência categoria | `EXPENSE`↔categoria de gasto, `INCOME`↔categoria de entrada; divergência → 400 | Dashboard depende disso |
| Filtros | `GET /v1/transactions?month=YYYY-MM` (obrigatório) `&accountId&categoryId&type&page&size` via JPA Specification | `(:param is null or ...)` com UUID quebra no Postgres/Hibernate 6 |
| Paginação | `PageResponse<T>` próprio (content, page, size, totalElements, totalPages), sort fixo `date desc, created_at desc` | Contrato estável (Page do Spring muda entre versões) |
| Response | Inclui `accountName`, `categoryName/Icon/Color` e `invoiceMonth` | Evita N chamadas do front na tabela |
| FK em uso | `DataIntegrityViolationException` → **409** "registro em uso" | Delete de conta/categoria com transações agora falha de forma amigável |

## Tasks

- **TASK-1 — Migration V3 + entidades**: `CardInvoice` (status OPEN/CLOSED/PAID), `Transaction`, repositórios.
- **TASK-2 — CardInvoiceService**: get-or-create por período com regra de fechamento/vencimento e clamp de fim de mês; testes unit cobrindo bordas (dia do fechamento, fevereiro, due ≤ closing).
- **TASK-3 — TransactionService + controller**: CRUD com validações (conta/categoria do usuário, coerência de kind, tipo reservado), vínculo/re-vínculo de fatura, search com Specification + paginação.
- **TASK-4 — Testes**: unit (service + invoice) e integração (fluxo completo com filtros, cartão criando fatura, 400/404/409/401); JaCoCo ≥90%.
- **TASK-5 — Verificação end-to-end**: `mvnw verify` verde; fluxo manual via API (compra no cartão antes/depois do fechamento caindo em faturas diferentes); PR com CI verde.

## Status

- [x] TASK-1 — Migration V3 + entidades
- [x] TASK-2 — CardInvoiceService
- [x] TASK-3 — TransactionService + controller
- [x] TASK-4 — Testes
- [x] TASK-5 — Verificação end-to-end

**Resultado da verificação (2026-07-09):**
- `mvnw verify`: 68 testes, JaCoCo ≥90% OK (9 CardInvoiceService com bordas de calendário, 10 TransactionService, 8 integração de fluxo)
- Manual contra o compose: compra 27/07 → fatura 2026-07; compra 28/07 (dia do fechamento) → fatura 2026-08; listagem por mês com nome de conta/categoria e paginação
- Correção durante os testes: expectativa errada minha para due_day ≤ closing_day em fevereiro (vencimento vai para o mês seguinte — a implementação estava certa)
- Delete de conta/categoria com transações agora responde 409 (DataIntegrityViolation → handler novo)
