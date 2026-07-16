# SDD — Sessão #16: Metas Financeiras

> **Data:** 2026-07-16 · **Pré-req:** #13 · **Branch:** `feature/metas-financeiras` (a partir de `develop`) · **Plano-mestre:** `docs/PLANO-SDD.md`
> **Fecha a Fase 2 — Investimentos** (junto com #13/#14/#15, ainda com PR aberto aguardando merge — sessão independente, branch a partir de `develop`).

## Objetivo

CRUD de metas financeiras (`goals`) e aportes mensais (`goal_contributions`), com cálculo automático do aporte mensal necessário para atingir a meta na data alvo, e tela com barras de progresso — reproduzindo o painel "Metas de patrimônio" do protótipo (`prototipo-dashboard.html`, página Investimentos, que mistura os dois; aqui viram sessão própria conforme o plano).

## Modelo de dados (`spec-app-financeiro.md`, seção 2)

```
goals(id, user_id, name, target_amount, target_date, created_at)
goal_contributions(id, goal_id, month, amount, created_at)
```

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Migration | **V9** (não V6 como o `PLANO-SDD.md` registrava — V6 já foi usada pela sessão #10/Orçamentos, e V7/V8 pelas sessões #13/#14 de Investimentos; texto do plano estava desatualizado) | próxima livre. |
| Aporte mensal necessário | `restante = max(target_amount − acumulado, 0)`; `mesesRestantes = meses entre o mês atual e target_date` (truncado, pode ser negativo se a data já passou); se `restante = 0` → `0`; senão se `mesesRestantes ≤ 0` → `restante` (tudo devido agora); senão `restante / mesesRestantes` (arredondado para cima, em centavos) | replica a regra 5 do `PLANO-SDD.md`; arredondar pra cima evita subestimar o aporte necessário (arredondar pra baixo deixaria a meta systematically atrasada). |
| Acumulado | soma de `goal_contributions.amount` da meta (não vem de `investments` — são fluxos registrados manualmente, como na aba "Metas Financeiras" da planilha) | a spec trata metas como um registro independente de aporte, não uma projeção sobre os investimentos reais. |
| Update de meta | `name`, `targetAmount` e `targetDate` editáveis (diferente de Investimento, que trava a `class`) | mudar o alvo/data de uma meta é uso normal (recalcula o aporte necessário) e não corrompe histórico, ao contrário da classe de um investimento. |
| Delete de meta | cascata (`ON DELETE CASCADE` em `goal_contributions.goal_id`) | mesmo raciocínio da sessão #13 — nada mais referencia `goals`. |
| Contribuição duplicada por mês | permitida (várias contribuições no mesmo mês são somadas), sem unique constraint | diferente de orçamento (1 por categoria/mês) — aportes podem vir em mais de uma vez no mês real. |
| Endpoints | `/v1/goals` (CRUD) + `/v1/goals/{id}/contributions` (create/list/delete) — relatório (`accumulated`, `requiredMonthlyContribution`, `progressPercentage`) embutido na resposta de `GET /v1/goals`, sem endpoint separado | só há uma "visão" de meta (não há filtro por mês como no orçamento), então não precisa de relatório à parte. |

## Tasks

- **TASK-1 — Migration V9 + entidades**: `Goal`, `GoalContribution`, repositórios.
- **TASK-2 — CRUD + cálculo**: `GoalService` (aporte necessário), `GoalController`.
- **TASK-3 — Testes backend**: cálculo (unit), service (mock), integração (`@SpringBootTest` + Testcontainers); JaCoCo ≥90%.
- **TASK-4 — Frontend**: tela `/metas` — cards/lista com barra de progresso e "Aporte necessário: R$ X/mês até MM/AAAA" (protótipo), CRUD de meta e de aporte.
- **TASK-5 — Testes web + verificação**: specs Angular (Karma ≥90/80/90/90) + verificação end-to-end no browser.

## Status

- [x] TASK-1 — Migration V9 + entidades
- [x] TASK-2 — CRUD + cálculo
- [x] TASK-3 — Testes backend
- [x] TASK-4 — Frontend
- [x] TASK-5 — Testes web + verificação

**Resultado da verificação (2026-07-16):**
- API: `mvnw verify` — **239 testes** (`RequiredContributionCalculatorTest`, `GoalServiceTest`, `GoalFlowIntegrationTest` novos, mais ajuste no `UserDataServiceTest` para LGPD), JaCoCo ≥90%, BUILD SUCCESS.
- Web: `npm run test:ci` — **152 testes**, cobertura 96,57/82,22/92,83/96,27% (acima de 90/80/90/90).
- **Verificação end-to-end real no browser** (API + Postgres via Docker Compose + `ng serve`): criada a meta "Reserva de emergência" (alvo R$ 12.000, dez/2026) — aporte necessário calculado corretamente em **R$ 2.400,00/mês** (5 meses restantes); registrado um aporte de R$ 7.200 — progresso atualizou para **60%** (barra `.bar` com `width: 60%` confirmado via DOM) e aporte necessário recalculado para **R$ 960,00/mês**; edição e exclusão (com cascata dos aportes) testadas com sucesso.
- **Gotcha da verificação**: uma instância antiga da API (rodando código de antes da sessão, sem `/v1/goals`) ficou presa na porta 8080 de uma verificação anterior (session #15) e não morreu ao `kill` do PID salvo (o `nohup` do Maven Wrapper spawna um processo filho) — causou um 500 real na primeira tentativa de criar a meta. Identificado via `netstat -ano` + `Stop-Process -Force` no PID correto da porta; nada relacionado ao código desta sessão.
