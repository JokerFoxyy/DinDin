# SDD — Sessão #13: Investimentos (backend)

> **Data:** 2026-07-15 · **Pré-req:** #2 (independente do MVP) · **Branch:** `feature/investimentos-backend` · **Plano-mestre:** `docs/PLANO-SDD.md`
> **Fase 2 — Investimentos.** Primeira sessão da fase; frontend fica para a #15.

## Objetivo

Backend de investimentos: CRUD de `investments` (agrupados por classe) e `investment_entries` (aporte/resgate/atualização de saldo), com cálculo de rentabilidade TWR simplificado por investimento e agregação por classe — sem UI ainda (sessão #15).

## Modelo de dados (`spec-app-financeiro.md`, seção 2)

```
investments(id, user_id, name, class, institution, created_at)
  class: RESERVA | RENDA_FIXA | RENDA_VARIAVEL

investment_entries(id, investment_id, date, type, amount, balance_after NULL, created_at)
  type: APORTE | RESGATE | ATUALIZACAO_SALDO
```

- `amount`: valor movimentado (aporte/resgate). Para `ATUALIZACAO_SALDO`, é ignorado no cálculo (a fonte de verdade é `balance_after`).
- `balance_after`: obrigatório em `ATUALIZACAO_SALDO` (saldo observado após a atualização); opcional/nulo nos outros dois tipos (não usado no cálculo, é só um registro informativo caso o usuário queira anotar).

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Migration | **V7** (V6 usada pela sessão #10) | próxima livre conforme `CLAUDE.md`. |
| Delete de investimento | **cascata** — apaga os `investment_entries` junto | diferente de conta/categoria (sessão #6), nada mais referencia `investments`; não há motivo pra bloquear com 409. |
| Cálculo de saldo atual | máquina de estados percorrendo as entries em ordem de `date` (empate por `id` de criação): `APORTE` soma, `RESGATE` subtrai, `ATUALIZACAO_SALDO` **substitui** o saldo corrente pelo `balanceAfter` informado | saldo sempre reflete a última atualização real, com aportes/resgates subsequentes ajustando por cima — mesma filosofia de "saldo é calculado" do `CLAUDE.md`. |
| Rentabilidade (TWR simplificado) | entre duas `ATUALIZACAO_SALDO` consecutivas: `fluxoLiquido = Σaportes − Σresgates` (no período); `rendimento = saldoAtual − saldoAnterior − fluxoLiquido`; `percentual = rendimento / saldoAnterior` (nulo se `saldoAnterior` ≤ 0 ou não houver duas atualizações) | extensão direta da fórmula da spec (`(saldo_atual - saldo_anterior - aportes) / saldo_anterior`), incluindo resgates no fluxo líquido para não distorcer o rendimento quando há resgate no período. |
| Rentabilidade por classe | agrega todas as entries de todos os investimentos da classe numa linha do tempo única (mesma máquina de estados, `saldoAnterior`/`saldoAtual` somados) | trata a classe como uma "carteira única" para efeito de rentabilidade agregada — consistente com o gráfico "carteira × CDI" da sessão #14. |
| Endpoints | `/v1/investments` (CRUD) e `/v1/investments/{id}/entries` (sub-recurso, CRUD sem update — só create/delete/list) + `/v1/investments/report` (agregado) | segue o padrão REST das sessões #10/#11. |
| Validação | `class` imutável após criação (trocar de classe distorce a série histórica de rentabilidade) — editar só `name`/`institution` | evita quebrar o cálculo agregado por classe silenciosamente. |

## Tasks

- **TASK-1 — Migration V7 + entidades**: `Investment`, `InvestmentEntry` (JPA), `AssetClass`/`EntryType` enums, repositórios.
- **TASK-2 — CRUD de investimentos**: `InvestmentService`/`InvestmentController` — create/list/update(nome/instituição)/delete (cascata).
- **TASK-3 — CRUD de entries**: create/list(por investimento, ordenado por data)/delete; validação `balanceAfter` obrigatório quando `type=ATUALIZACAO_SALDO`.
- **TASK-4 — Cálculo TWR**: `InvestmentReturnCalculator` (saldo atual + rentabilidade do último período) por investimento e agregado por classe; endpoint `GET /v1/investments/report`.
- **TASK-5 — Testes + verificação**: unit (calculator, service) + integração (`@SpringBootTest` + Testcontainers); JaCoCo ≥90%; `mvnw verify` verde.

## Status

- [x] TASK-1 — Migration V7 + entidades
- [x] TASK-2 — CRUD de investimentos
- [x] TASK-3 — CRUD de entries
- [x] TASK-4 — Cálculo TWR
- [x] TASK-5 — Testes + verificação

**Resultado da verificação (2026-07-14):**
- `mvnw verify` — **207 testes** (`InvestmentReturnCalculatorTest`, `InvestmentServiceTest`, `InvestmentFlowIntegrationTest` novos, mais ajuste no `UserDataServiceTest` para a exclusão LGPD de investimentos), JaCoCo ≥90%, BUILD SUCCESS.
- Endpoints verificados via `TestRestTemplate` contra a API real (Testcontainers Postgres): CRUD de `/v1/investments` e `/v1/investments/{id}/entries`, cálculo de rentabilidade em `/v1/investments/report` (aporte no meio do período corretamente descontado do rendimento), 404 ao acessar investimento de outro usuário, 400 quando `ATUALIZACAO_SALDO` não informa `balanceAfter`.
- `DELETE /v1/investments/{id}` usa `ON DELETE CASCADE` no banco (`investment_entries.investment_id`) — apaga as entries junto, sem bloqueio 409 (diferente de conta/categoria).
- LGPD (`UserDataService`): investimentos entram na exportação (`GET /account/export`) e na exclusão (`DELETE /account`), antes do `refreshTokenRepository`.
