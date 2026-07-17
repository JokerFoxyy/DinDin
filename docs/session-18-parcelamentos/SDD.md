# SDD — Sessão #18: Parcelamentos

> **Data:** 2026-07-17 · **Pré-req:** #9 · **Branch:** `feature/parcelamentos` · **Plano-mestre:** `docs/PLANO-SDD.md`
> **Fase 3 — Qualidade de vida.**

## Objetivo

Compra em N× gera N transações futuras vinculadas (uma por mês), cada uma seguindo a mesma regra de vínculo à fatura já existente (sessão #6/#9) — sem duplicar lógica de fatura.

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Significado de `amount` no parcelamento | é o **valor de cada parcela**, não o total da compra | mesma convenção já usada na planilha original e registrada na sessão #12 ("Valor já é a parcela do mês") — consistente com o resto do sistema, evita lógica de rateio/arredondamento de centavos. |
| Datas das parcelas | `date`, `date+1 mês`, `date+2 meses`... (`LocalDate.plusMonths` já clampa dia inexistente, ex. 31/jan + 1 mês = 28/fev) | `LocalDate` já resolve isso nativamente, sem precisar de helper de clamp. |
| Vínculo de fatura | cada parcela passa pelo `invoiceIdFor` normal (mesma regra de `closing_day` da sessão #9) — não um vínculo especial de parcelamento | cada parcela é uma compra "no cartão" daquele mês; a fatura de cada mês já resolve isso sozinha. |
| Modelo de dados | `installment_group_id` (UUID, mesmo para as N), `installment_number` (1..N), `installment_count` (N) em `transactions` | permite exibir "3/12" e agrupar para exclusão, sem precisar de tabela separada. |
| Escopo | só `type=EXPENSE` pode ter `installments > 1` (400 caso contrário) | parcelamento de entrada não é um caso de uso do app. |
| Edição | `PUT` de uma parcela edita só aquela transação (não recalcula as demais) — mesmo comportamento de uma transação normal | recalcular a série inteira na edição é complexidade desproporcional ao pedido do plano ("gera N transações vinculadas", não "mantém sincronizadas"). |
| Exclusão | `DELETE /v1/transactions/{id}?scope=group` remove essa parcela **e todas as futuras do mesmo grupo** (`date >=` a da parcela clicada); sem o parâmetro (`scope` ausente/`single`), comportamento atual inalterado (remove só a transação) | cancelar uma compra parcelada normalmente significa parar de pagar as parcelas restantes; preserva o histórico das parcelas já pagas/passadas. |
| Resposta da criação | `POST /v1/transactions` continua retornando **uma** `TransactionResponse` (a primeira parcela) | mantém o contrato existente; a tela recarrega a lista do mês depois de criar, então não precisa da lista completa das N parcelas na resposta. |

## Tasks

- **TASK-1 — Backend**: migration V11; `Transaction` ganha `installmentGroupId/Number/Count` + factory `installment(...)`; `TransactionRequest.installments` (opcional, 1–60); `TransactionService.create` gera N transações quando `installments > 1`; `TransactionService.delete` aceita `scope=group`.
- **TASK-2 — Testes backend**: geração das N parcelas (datas, fatura por mês, mesma classe), 400 quando `type != EXPENSE`, exclusão em grupo (só futuras) vs. exclusão simples; JaCoCo ≥90%.
- **TASK-3 — Frontend**: campo "Parcelas" no formulário de lançamento (só habilitado com tipo Gasto), preview "N× de R$ X"; badge "n/N" na listagem; ação "Excluir esta e as próximas" quando a transação pertence a um grupo.
- **TASK-4 — Testes web + verificação**: specs Angular (Karma ≥90/80/90/90) + verificação end-to-end no browser (compra parcelada em cartão de crédito, conferindo a fatura de cada mês).

## Status

- [x] TASK-1 — Backend
- [x] TASK-2 — Testes backend
- [x] TASK-3 — Frontend
- [x] TASK-4 — Testes web + verificação

**Resultado da verificação (2026-07-17):**
- API: `mvnw verify` — **252 testes** (geração de N parcelas com datas consecutivas, vínculo de fatura por parcela, 400 para parcelamento em entrada, exclusão em grupo só das futuras), JaCoCo ≥90%, BUILD SUCCESS.
- Web: `npm run test:ci` — **206 testes**, cobertura 96,6/84,86/92,96/96,52% (acima de 90/80/90/90).
- **Verificação end-to-end real no browser**: compra "Notebook" de R$500 em 6x no cartão Nubank (fechamento dia 28) — parcela 1/6 lançada em 09/07 vinculada à fatura de julho, parcela 2/6 em 09/08 vinculada à fatura de agosto (cada parcela passou pela regra normal de vínculo de fatura da sessão #9, sem lógica especial); "Excluir esta e as próximas" a partir da parcela 2/6 removeu as parcelas futuras e preservou a 1/6 intacta.
