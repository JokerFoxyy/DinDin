# SDD — Sessão #7: Transações (frontend)

> **Data:** 2026-07-09 · **Pré-req:** #6 ✅ · **Branch:** `feature/transacoes-frontend` · **Plano-mestre:** `docs/PLANO-SDD.md`

## Objetivo

Tela de Transações fiel ao protótipo: tabela mensal com tags coloridas de categoria, month-picker na topbar, modal de lançamento rápido (data = hoje, última conta usada lembrada), edição/exclusão, filtros por conta/categoria/tipo e paginação.

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Locale | `registerLocaleData(pt)` + `LOCALE_ID='pt-BR'` no app.config | `currency:'BRL'` e datas dd/MM nativos do Angular |
| Month-picker | Componente `shared/month-picker` com `model<string>('YYYY-MM')` e label pt-BR (Intl) | Reuso no Dashboard (#11) e Fixos (#8) |
| Lançamento rápido | Modal no próprio componente; defaults: data hoje, type EXPENSE, **última conta usada** (`localStorage dindin.lastAccount`), primeira categoria do kind | Spec Fase 1: "lançamento rápido" |
| Categorias no form | Filtradas pelo type selecionado (gasto↔EXPENSE, entrada↔INCOME) | Espelha a validação do backend (400) |
| Valores | `amount` sempre positivo; INCOME verde com `+`, EXPENSE cor padrão | Sinal é visual, dado é positivo (regra de ouro) |
| Fatura | Compra de cartão mostra "fatura MMM/yy" na célula da conta | Torna a regra da #6 visível ao usuário |
| Filtros/paginação | Selects (Todas/Todos) + ‹ › com "página X de Y"; mudança de filtro volta à página 0 | Contrato do `GET /v1/transactions` |
| Serviços | `features/transactions/transaction.service.ts`; reusa Account/CategoryService da settings | DRY |

## Tasks

- **TASK-1 — Infra de UI**: locale pt-BR, componente month-picker (+spec).
- **TASK-2 — TransactionService (web)** com models e spec.
- **TASK-3 — Tela**: tabela + filtros + paginação + modal criar/editar + excluir.
- **TASK-4 — Specs da tela**: defaults do lançamento rápido, filtros, paginação, edição, exclusão, erros; thresholds 90/80/90/90 mantidos.
- **TASK-5 — Verificação end-to-end**: fluxo real no browser contra API (criar no cartão → ver fatura; editar; excluir; navegar meses; filtrar); PR com CI verde.

## Status

- [x] TASK-1 — Infra de UI
- [x] TASK-2 — TransactionService (web)
- [x] TASK-3 — Tela
- [x] TASK-4 — Specs da tela
- [x] TASK-5 — Verificação end-to-end

**Resultado da verificação (2026-07-09):**
- `npm run test:ci`: 68 testes, cobertura 98,9/84,7/96,4/98,9; `build:prod` OK
- Browser (stack completo, usuário fatura-teste): tabela de julho com hint de fatura correto
  (compra 27/07 → "fatura jul./26", 28/07 → "fatura ago./26"); modal com defaults (hoje, última
  conta, primeira categoria do kind); criação apareceu na lista e gravou `dindin.lastAccount`;
  junho vazio; filtro Entradas → 0; exclusão OK; screenshot fiel ao protótipo
- Bugs de percurso: (1) teste com data UTC vs local (corrigido no spec — componente usa data local,
  correto); (2) NG0203 no dev server por cache do Vite com chunks misturados após mudar app.config —
  **resolver com restart do dev server**, não é bug de código
