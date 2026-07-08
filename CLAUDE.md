# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## O que é este projeto

FinanceIA (FinApp): app de gestão financeira pessoal que substitui a Planilha_Gastos_2026 — transações, contas/cartões com fatura, fixos recorrentes, orçamentos, dashboard, investimentos e metas. Uso pessoal com potencial de virar SaaS.

**Estado atual: repositório em fase de planejamento — ainda não há código.** Antes de implementar qualquer coisa, leia:

- `docs/PLANO-SDD.md` — documento-mestre: stack, sessões planejadas (#1–#21), grafo de dependências e regras de negócio. **É a fonte de verdade do roadmap.**
- `../spec-app-financeiro.md` (fora do repo, na pasta pai) — spec completa com modelo de dados e fases.
- `../prototipo-dashboard.html` — protótipo visual de referência (tema dark, layout, gráficos).

## Fluxo de trabalho (SDD)

O desenvolvimento é organizado em sessões numeradas (mesmo padrão do projeto ContratoIA). Ao iniciar a sessão #NN:

1. Criar `docs/session-NN-nome/SDD.md` detalhando as tasks antes de codar.
2. Implementar task a task, com verificação end-to-end como última task.
3. Atualizar o status da sessão em `docs/PLANO-SDD.md` ao concluir.

## Arquitetura (planejada)

Monorepo:

```
api/    Java 21 + Spring Boot 3.5 + Maven — pacotes POR FEATURE (auth/, account/,
        category/, transaction/, invoice/, recurring/, budget/, goal/, investment/,
        dashboard/, importer/, common/), cada um com controller, service, repository e DTOs
web/    Angular 20 (standalone components, signals) + Tailwind + ng2-charts (Chart.js)
infra/  docker-compose.yml (Postgres 16, api, caddy) e scripts
docs/   PLANO-SDD.md + SDDs por sessão
```

- Banco: PostgreSQL com Flyway (migrations V1–V7 mapeadas no plano).
- Auth: Spring Security + JWT próprio (não Keycloak).
- Frontend consome só a API REST (OpenAPI/springdoc) — mobile futuro reusa o contrato.

## Regras não negociáveis

- **Dinheiro:** `BigDecimal` no Java, `NUMERIC(14,2)` no Postgres. Nunca float/double.
- **Datas:** `LocalDate` (sem timezone para datas de transação).
- **Saldos são calculados, nunca armazenados:** `saldo(mês) = saldo(mês−1) + entradas − gastos`.
- **Toda transação tem conta obrigatória**; lançamento em cartão de crédito vincula-se à fatura (`card_invoice`) do período conforme o `closing_day` do cartão.
- **Fechamento de fatura:** diferença entre total lançado e valor declarado vira transação `INVOICE_ADJUSTMENT` automática, reduzida conforme o usuário detalha os gastos reais.
- UI segue o tema dark do protótipo (variáveis CSS `--bg:#0d1117`, `--card:#161b22`, `--accent:#4f8ef7` etc.).
- Textos de UI em pt-BR.

## Comandos

Ainda não há build configurado. Quando as sessões #1–#3 criarem os projetos, os comandos esperados serão:

- API: `cd api && ./mvnw verify` (testes usam Testcontainers — Docker precisa estar rodando); subir dependências com `docker compose -f infra/docker-compose.yml up -d postgres`.
- Web: `cd web && npm start` (dev server), `npm test` (Karma/Jasmine), `npm run build`.

Atualize esta seção com os comandos reais assim que existirem.
