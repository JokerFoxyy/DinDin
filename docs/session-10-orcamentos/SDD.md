# SDD — Sessão #10: Orçamentos

> **Data:** 2026-07-13 · **Pré-req:** #6 ✅ · **Branch:** `feature/orcamentos` · **Plano-mestre:** `docs/PLANO-SDD.md`

## Objetivo

Permitir que o usuário defina um valor orçado por categoria de gasto em um dado mês, e comparar com o valor realmente gasto (realizado) naquele mês, exibindo barra de progresso (vermelha ao estourar).

## Migration

**V6** (`budgets`). Nota de numeração: o plano-mestre original previa V5 para `budgets`, mas a sessão #8 (Fixos Recorrentes, PR #15) já reservou V5 para `recurring_transactions` e ainda não foi mergeada em `develop` no momento desta sessão. Para evitar colisão de versão Flyway quando o PR #15 for mergeado, esta sessão usa **V6**.

```sql
CREATE TABLE budgets (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id),
    category_id UUID NOT NULL REFERENCES categories (id),
    month       DATE NOT NULL, -- primeiro dia do mês
    amount      NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_budgets_user_category_month UNIQUE (user_id, category_id, month)
);
CREATE INDEX idx_budgets_user_month ON budgets (user_id, month);
```

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Categoria elegível | somente categorias `kind = EXPENSE` | orçamento só faz sentido para gasto |
| Unicidade | `(user_id, category_id, month)` único | um orçamento por categoria por mês |
| Realizado | soma de `transactions.amount` do tipo `EXPENSE` na categoria, no intervalo do mês (`month.atDay(1)`..`month.atEndOfMonth()`) | mesma janela usada em `TransactionSpecifications` |
| Endpoint de relatório | `GET /v1/budgets?month=` retorna, para cada orçamento do mês, `{id, categoryId, categoryName, categoryIcon, categoryColor, budgeted, spent, percentage, over}` | um único endpoint cobre listagem + comparação |
| Edição | somente `amount` é editável via `PUT /{id}` (categoria/mês são imutáveis; para mudar, apagar e recriar) | evita reabrir a chave de unicidade |
| Percentual | `percentage = spent / budgeted * 100` (0 se budgeted=0, nunca ocorre pois amount>0); `over = spent > budgeted` | usado para colorir a barra |
| Escopo | orçamento pertence ao usuário via `categoryRepository.findByIdAndUserId`; unicidade é validada por usuário | mesma regra dos outros recursos |
| Endpoints | `GET /v1/budgets?month=`, `POST /v1/budgets`, `PUT /v1/budgets/{id}`, `DELETE /v1/budgets/{id}` | REST |

## Tasks

- **TASK-1 — Migration** `V6__create_budgets.sql`.
- **TASK-2 — Domínio**: `Budget` entity, `BudgetRepository` (finders por mês/categoria, `existsByUserIdAndCategoryIdAndMonth`).
- **TASK-3 — BudgetService**: `report(userId, month)`, `create`, `updateAmount`, `delete`; soma de gastos via nova query em `TransactionRepository`.
- **TASK-4 — BudgetController** `/v1/budgets`.
- **TASK-5 — Testes backend**: unit (relatório com/sem gasto, percentual, duplicidade, categoria de INCOME rejeitada, escopo) + integração (fluxo criar→gastar→relatório→editar→excluir + 400/404/409/401); JaCoCo ≥90%.
- **TASK-6 — Frontend**: tela "Orçamentos" (month-picker, cards/tabela com barra orçado×realizado, form criar/editar, excluir); rota + sidebar.
- **TASK-7 — Verificação end-to-end**: `mvnw verify` + `npm run test:ci` verdes; browser (criar orçamento → lançar gasto → ver barra progredir/estourar → editar valor → excluir); PR com CI verde (entregue ao usuário).

## Status

- [x] TASK-1 — Migration
- [x] TASK-2 — Domínio
- [x] TASK-3 — BudgetService
- [x] TASK-4 — BudgetController
- [x] TASK-5 — Testes backend
- [x] TASK-6 — Frontend
- [x] TASK-7 — Verificação end-to-end

**Resultado da verificação (2026-07-13):**
- API: `mvnw verify` — **126 testes** (11 unit `BudgetServiceTest` + 7 integração `BudgetFlowIntegrationTest`, resto pré-existente), JaCoCo com todos os checks de cobertura atendidos, BUILD SUCCESS
- Web: `npm run test:ci` — **89 testes**, cobertura 98,21/86,48/94,44/98,05% (statements/branches/functions/lines), acima dos thresholds 90/80/90/90; `shell.spec.ts` ajustado para 7 itens de navegação (novo "Orçamentos")
- Browser (stack completo: docker compose + API local + `ng serve`): criada categoria "Mercado" (EXPENSE) → orçamento R$500 em Orçamentos → lançada transação R$300 em Transações → voltando a Orçamentos, Realizado atualizou para R$300 (60%, barra azul `rgb(79,142,247)`, `over:false`) → lançada 2ª transação R$300 (total R$600) → Realizado 120%, barra vermelha `rgb(248,81,73)` (`over:true`), cor do valor "Realizado" também vermelha → editado orçamento para R$700 (86%, volta ao normal) → excluído (lista volta a "Nenhum orçamento definido neste mês")
- Migration usada: **V6** (V5 já reservada pela sessão #8/PR #15, ainda não mergeada em `develop`)
