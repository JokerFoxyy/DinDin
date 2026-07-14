# SDD — Sessão #8: Fixos Recorrentes

> **Data:** 2026-07-11 · **Pré-req:** #6 ✅ · **Branch:** `feature/fixos-recorrentes` · **Plano-mestre:** `docs/PLANO-SDD.md`

## Objetivo

Lançamentos fixos (Spotify, academia, aluguel...): CRUD dos templates, materialização automática mensal em `transactions` com flag "pago?", e tela Fixos com o checkbox.

## Migration

**V5** (V4 foi `refresh_tokens` na sessão #S):
- `recurring_transactions` (template do fixo)
- `ALTER transactions ADD recurring_id` (FK nullable → liga a transação materializada ao fixo)
- `ALTER transactions ADD paid` (boolean, default `true`; fixos materializados nascem `false`)

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| `type` do fixo | EXPENSE · INCOME (não INVOICE_ADJUSTMENT) | Fixo é gasto ou entrada |
| `day_of_month` | 1–31, clampado ao fim do mês na materialização | Fevereiro/dia 31 |
| `end_date` | opcional; não materializa se a data da ocorrência for depois do end_date | Fixo com prazo |
| `paid` | coluna nova em `transactions`, default `true` | Manual = já pago; fixo materializado = pendente (`false`) até o usuário marcar |
| Vínculo de fatura | Fixo em cartão de crédito também vincula à fatura (reusa `CardInvoiceService`) | Coerência com a regra da #6 |
| Idempotência | Não materializa se já existe transação com aquele `recurring_id` no mês | Job/tela podem rodar várias vezes |
| Materialização | Serviço `RecurringMaterializationService`; job `@Scheduled` mensal materializa todos os fixos ativos + endpoint `POST /v1/recurring/materialize?month=` para o mês corrente sob demanda | Automático **e** testável |
| Ocorrências | `GET /v1/recurring/occurrences?month=` retorna cada fixo ativo do mês com status {materializado, transactionId, paid} | Alimenta a tela |
| "pago?" | `PATCH /v1/transactions/{id}/paid` `{paid}` | Toggle no checkbox |
| Scheduling | `@EnableScheduling` na ApiApplication; job testado via o serviço (não pela cadência) | — |

## Tasks

- **TASK-1 — Migration V5 + entidade/DTOs**: `recurring_transactions`, alter `transactions`, `RecurringTransaction` entity/repo, ajustes em `Transaction` (recurringId, paid).
- **TASK-2 — CRUD backend**: `RecurringService` + `RecurringController` (`/v1/recurring`), validações (conta/categoria do usuário, kind coerente com type).
- **TASK-3 — Materialização**: `RecurringMaterializationService` (materializa ocorrência do mês, idempotente, com vínculo de fatura), job `@Scheduled`, endpoints `POST /materialize` e `GET /occurrences`; `PATCH /transactions/{id}/paid`.
- **TASK-4 — Testes backend**: unit (service + materialização com bordas: clamp, end_date, idempotência, cartão) e integração (CRUD + materialize + occurrences + paid + 401/404/400); JaCoCo ≥90%.
- **TASK-5 — Frontend**: tela Fixos (month-picker, lista de fixos com tag/valor/dia, checkbox "pago?" do mês, form criar/editar, ativar/excluir); `RecurringService` (web) + specs.
- **TASK-6 — Verificação end-to-end**: `mvnw verify` + `npm run test:ci` verdes; browser (criar fixo → materializar mês → marcar pago → aparece em Transações como pago); PR com CI verde (entregue ao usuário para aprovar/mergear).

## Status

- [x] TASK-1 — Migration V5 + entidade
- [x] TASK-2 — CRUD backend
- [x] TASK-3 — Materialização
- [x] TASK-4 — Testes backend
- [x] TASK-5 — Frontend
- [x] TASK-6 — Verificação end-to-end

**Resultado da verificação (2026-07-12):**
- API: `mvnw verify` — **123 testes**, JaCoCo ≥90% (RecurringService, RecurringMaterializationService com bordas de calendário/idempotência/cartão/job, RecurringFlow integração)
- Web: `npm run test:ci` — **94 testes**, cobertura 97,8/85,4/93,6/97,6; `build:prod` OK
- Browser (stack completo): criar fixo "Spotify dia 10" → "Gerar lançamentos do mês" materializou (checkbox "Pendente") → marcar "Pago" (label vira "Pago", API confirma `paid:true`) → a transação materializada aparece em Transações (10/07, R$ 27,90); idempotência confirmada (materializar de novo não duplica)
- Nota: `PATCH` trocado por `PUT /transactions/{id}/paid` (TestRestTemplate/SimpleClientHttpRequestFactory não faz PATCH)
- LGPD: exclusão de conta agora apaga também `recurring_transactions` (ordem FK-safe atualizada)
