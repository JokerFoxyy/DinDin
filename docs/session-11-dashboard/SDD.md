# SDD — Sessão #11: Dashboard Mensal + Panorama Anual

> **Data:** 2026-07-13 · **Pré-req:** #7, #10 ✅ · **Branch:** `feature/dashboard` · **Plano-mestre:** `docs/PLANO-SDD.md`

## Objetivo

Tela inicial (`/dashboard`) com visão consolidada do mês: entradas, gastos, saldo do mês, saldo acumulado, gastos por categoria (donut), orçado vs. realizado (reaproveitando a sessão #10) e panorama anual (entradas × gastos, barras).

## Migration

**Nenhuma** — reaproveita `transactions` e `budgets` já existentes.

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Saldo | calculado, nunca armazenado — `saldo(mês) = entradas(mês) − gastos(mês)`; `saldo acumulado = Σentradas − Σgastos` desde 1970-01-01 até o fim do mês selecionado | regra de ouro do projeto (CLAUDE.md) |
| Gastos por categoria | nova query `sumExpensesByCategoryForMonth` (sem filtro de `categoryIds`, diferente da usada por Orçamentos) | donut precisa de todas as categorias com gasto no mês, não só as orçadas |
| Orçado × realizado | reaproveita `BudgetService.report(userId, month)` via injeção direta | evita duplicar a lógica de reconciliação de ajuste de fatura já implementada na sessão #10 |
| Panorama anual | série de Jan até o mês selecionado (não 12 meses fixos) — um `sumByTypeAndDateBetween` por mês do intervalo | mesma leitura visual do protótipo (`Jan..Jun` em vez de ano completo) |
| Gráficos | Chart.js direto (`chart.js/auto`), sem `ng2-charts` | `ng2-charts` 7+ exige `@angular/cdk` como peer dependency (não usado no projeto); Chart.js puro evita essa dependência extra |
| Timing de render | `afterNextRender(callback, {injector})` em vez de `setTimeout(0)` | `ViewChild` de um `<canvas>` dentro de `@if` só existe após o Angular atualizar o DOM; `setTimeout(0)` executava antes disso (`hasCanvas=false` em debug) |
| Animação dos gráficos | `animation: false` em ambos os charts | a animação inicial (rotate do donut) depende de `requestAnimationFrame`, que não progride de forma confiável no navegador automatizado de verificação — desabilitar também deixa a UI mais previsível para testes |
| Aspect ratio | `maintainAspectRatio: false` em ambos | sem isso o Chart.js força proporção quadrada (donut) ou 2:1 (barra), estourando a altura fixa de 240px do `.chart-box` |
| Endpoints | `GET /v1/dashboard/summary?month=`, `GET /v1/dashboard/annual?month=` | REST, mesmo padrão dos demais controllers |

## Tasks

- **TASK-1 — Backend**: `TransactionRepository.sumByTypeAndDateBetween` + `sumExpensesByCategoryForMonth`; `dashboard/` (Controller, Service, DTOs) reaproveitando `BudgetService`.
- **TASK-2 — Testes backend**: unit (`DashboardServiceTest`) + integração (`DashboardFlowIntegrationTest`: saldo do mês/acumulado, categorySpend, budgetReport, série anual, 401); JaCoCo ≥90%.
- **TASK-3 — Frontend**: `chart.js` instalado (sem `ng2-charts`); tela Dashboard com 4 cards, donut de categorias, tabela orçado/realizado e barra anual, usando `MonthPicker`.
- **TASK-4 — Testes web**: `dashboard.service.spec.ts` + `dashboard.spec.ts`; removida a entrada "Dashboard" de `pages.spec.ts` (não é mais placeholder).
- **TASK-5 — Verificação end-to-end**: `mvnw verify` + `npm run test:ci` verdes; browser (criar conta/categorias → lançar entrada e gasto → criar orçamento → conferir cards, tabela e ambos os `<canvas>` com pixels desenhados via `getImageData`); PR com CI verde (entregue ao usuário).

## Status

- [x] TASK-1 — Backend
- [x] TASK-2 — Testes backend
- [x] TASK-3 — Frontend
- [x] TASK-4 — Testes web
- [x] TASK-5 — Verificação end-to-end

**Resultado da verificação (2026-07-13):**
- API: `mvnw verify` — **168 testes** (`DashboardServiceTest` + `DashboardFlowIntegrationTest` novos), JaCoCo ≥90%, BUILD SUCCESS
- Web: `npm run test:ci` — **126 testes**, cobertura 96,72/85,57/92,01/96,44% (acima de 90/80/90/90)
- Browser (stack completo): Entradas R$5.000, Gastos R$300, Saldo do mês R$4.700, Saldo acumulado R$4.700 — todos corretos; tabela orçado/realizado mostrando Mercado (R$500 orçado, R$300 gasto); ambos os `<canvas>` (donut + barras) confirmados com pixels não-transparentes via `getImageData` (screenshot indisponível neste ambiente de verificação)
- Bug encontrado e corrigido durante a verificação: gráficos não renderizavam porque o `ViewChild` do `<canvas>` (dentro de `@if`) ainda não existia no momento do `setTimeout(0)` — trocado por `afterNextRender`; e a animação inicial do donut nunca completava (depende de `requestAnimationFrame`, que não progride de forma confiável no browser de automação) — corrigido com `animation:false`
