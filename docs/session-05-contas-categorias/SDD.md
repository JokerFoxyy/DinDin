# SDD — Sessão #5: Contas & Categorias

> **Data:** 2026-07-09 · **Pré-req:** #3 ✅ · **Branch:** `feature/contas-categorias` · **Plano-mestre:** `docs/PLANO-SDD.md`

## Objetivo

CRUD completo de contas/cartões e categorias (API + tela Configurações), escopado por usuário autenticado. É a base para transações (#6): toda transação exigirá uma conta e uma categoria destas.

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Migration V2 | `accounts` + `categories`, FK para `users`, CHECKs de domínio | Espelho da spec (seção 2) |
| `accounts.type` | CHECKING · CREDIT_CARD · CASH | Da spec; "Uniclass", "Nubank", "Dinheiro"... |
| Regra cartão | `closing_day`/`due_day` (1–31) **obrigatórios se CREDIT_CARD, forçados a null caso contrário** | Precisos para o vínculo de fatura na #6; normalizar evita lixo |
| `categories` | name + icon (emoji) + color (`#rrggbb`) + kind (EXPENSE·INCOME), únicos por (user, name, kind) | Como a planilha/protótipo; duplicata → 409 |
| Escopo por usuário | Toda query filtra por `user_id` do token; registro de outro usuário → **404** (não 403) | Não vazar existência de recursos |
| Erros novos | `NotFoundException`→404, `DuplicateResourceException`→409, `BusinessException`→400 | Ampliação do contrato do GlobalExceptionHandler |
| Delete | Físico nesta sessão | Transações (#6) criarão FK; a partir de lá delete com vínculo → 409 |
| Front | `features/settings` com `AccountsPanel` + `CategoriesPanel` (form inline + tabela) | Componentes separados = testáveis e reusáveis |
| Endpoints | `GET/POST /v1/accounts`, `PUT/DELETE /v1/accounts/{id}`; idem `/v1/categories` | REST padrão |

## Tasks

- **TASK-1 — Backend accounts**: migration V2, entity/repo/service/controller/DTOs com regra de cartão, testes unit.
- **TASK-2 — Backend categories**: entity/repo/service/controller/DTOs com unicidade, testes unit.
- **TASK-3 — Integração backend**: teste de fluxo (CRUD + 401/404/409) com Testcontainers; JaCoCo ≥90%.
- **TASK-4 — Frontend**: models + `AccountService`/`CategoryService`, painéis na tela Configurações (listar, criar, editar, excluir; campos de fatura só para cartão), pt-BR, tema do protótipo.
- **TASK-5 — Testes frontend**: specs de services e painéis; thresholds 90/80/90/90 mantidos.
- **TASK-6 — Verificação end-to-end**: `mvnw verify` + `npm run test:ci` verdes; fluxo real no browser (criar conta corrente, cartão com fechamento/vencimento, categorias com cor/ícone, editar, excluir); PR com CI verde.

## Status

- [x] TASK-1 — Backend accounts
- [x] TASK-2 — Backend categories
- [x] TASK-3 — Integração backend
- [x] TASK-4 — Frontend Configurações
- [x] TASK-5 — Testes frontend
- [x] TASK-6 — Verificação end-to-end

**Resultado da verificação (2026-07-09):**
- API: `mvnw verify` — 41 testes, JaCoCo ≥90% OK (8 AccountService, 8 CategoryService, 6 integração de fluxo)
- Web: `npm run test:ci` — 51 testes, cobertura 98,9/83,3/96,2/98,8
- Browser (stack completo): criou conta corrente e cartão (fechamento 28/vencimento 7 exibidos), categoria com emoji+cor, duplicata bloqueada com "Categoria já existe" (409), edição renomeou com reordenação, exclusão removeu; screenshot ok; nenhum erro de rede não-intencional
