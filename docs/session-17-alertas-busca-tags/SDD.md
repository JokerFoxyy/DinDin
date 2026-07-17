# SDD — Sessão #17: Alertas de Orçamento + Busca e Tags

> **Data:** 2026-07-16 · **Pré-req:** #11 · **Branch:** `feature/alertas-busca-tags` · **Plano-mestre:** `docs/PLANO-SDD.md`
> **Fase 3 — Qualidade de vida.** Primeira sessão da fase.

## Objetivo

Três melhorias de qualidade de vida sobre o que já existe: (1) alerta visível quando um orçamento estoura, (2) busca full-text + filtros avançados nas transações, (3) tags livres por transação.

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Alerta de orçamento | `GET /v1/budgets/alerts?month=YYYY-MM` → subconjunto de `BudgetService.report()` filtrado a `over=true`; frontend busca isso no `Shell` (layout) e mostra um badge numérico no item de nav "Orçamentos" | reaproveita o cálculo já existente (sessão #10) em vez de duplicar regra; "alerta no app" do plano é satisfeito por um badge, sem necessidade de push/e-mail. |
| Tags livres | `@ElementCollection` (`Set<String>`) em vez de coluna `text[]` nativa do Postgres | `CriteriaBuilder.isMember` funciona nativamente com uma collection JPA mapeada, permitindo compor o filtro por tag com a `Specification` dinâmica já existente (`TransactionSpecifications`) sem SQL nativo. Tabela auxiliar `transaction_tags(transaction_id, tag)`. |
| Normalização de tag | trim + lowercase antes de persistir, dedupe automático (é um `Set`) | evita "Viagem" e "viagem" virarem tags diferentes por acidente. |
| Busca full-text | parâmetro `q` opcional em `GET /v1/transactions`, `LIKE lower(description) like %q%` | é a única coluna de texto livre relevante pra buscar (categoria/conta já são filtros próprios); sem necessidade de extensão `pg_trgm`/tsvector pro volume de uso pessoal. |
| Filtro por tag | parâmetro `tag` opcional (uma tag por vez) em `GET /v1/transactions`, via `cb.isMember` | filtro simples; múltiplas tags simultâneas ficam de fora do escopo (não pedido pelo plano). |
| Migration | **V10** | próxima livre (V9 foi metas, sessão #16). |

## Tasks

- **TASK-1 — Tags livres (backend)**: migration V10 (`transaction_tags`), `Transaction.tags` (`@ElementCollection`), `TransactionRequest`/`TransactionResponse` com `tags`, `TransactionService` normaliza e persiste.
- **TASK-2 — Busca + alerta (backend)**: parâmetros `q`/`tag` no `TransactionController`/`TransactionSpecifications`; `GET /v1/budgets/alerts`.
- **TASK-3 — Testes backend**: cobertura de tags (dedupe/normalização), busca por texto, filtro por tag, alerta; JaCoCo ≥90%.
- **TASK-4 — Frontend**: campo de busca + filtro de tag na tela de Transações; campo de tags (chips) no formulário de lançamento; badge de alerta no item "Orçamentos" do `Shell`.
- **TASK-5 — Testes web + verificação**: specs Angular (Karma ≥90/80/90/90) + verificação end-to-end no browser.

## Status

- [x] TASK-1 — Tags livres (backend)
- [x] TASK-2 — Busca + alerta (backend)
- [x] TASK-3 — Testes backend
- [x] TASK-4 — Frontend
- [x] TASK-5 — Testes web + verificação

**Resultado da verificação (2026-07-16):**
- API: `mvnw verify` — testes de tags (normalização/dedupe), busca por texto, filtro por tag e alerta de orçamento, JaCoCo ≥90%, BUILD SUCCESS.
- Web: `npm run test:ci` — **197 testes**, cobertura 96,57/84/92,92/96,5% (acima de 90/80/90/90). Removido `pages.spec.ts` e `shared/page-placeholder.ts` (código morto — as sessões #15/#16, já mergeadas em `develop`, substituíram os últimos placeholders que esse teste cobria; o teste quebrava com "No provider found for HttpClient" porque `Investments` deixou de ser um placeholder simples).
- **Verificação end-to-end real no browser**: criado lançamento "Cinema com amigos" com tags `" Lazer , Amigos, lazer "` — normalizado e deduplicado corretamente para `#amigos #lazer`; busca por texto (`q=padaria` → 0 resultados, sem termo → volta a mostrar) e filtro por tag (`tag=viagem` → 0, `tag=amigos` → 1) funcionaram; orçamento de R$50 em "Lazer" com gasto de R$80 no mês disparou o badge de alerta "1" no item "Orçamentos" do menu lateral (confirmado via DOM).
