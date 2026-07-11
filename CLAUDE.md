# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## O que é este projeto

DinDin: app de gestão financeira pessoal que substitui a Planilha_Gastos_2026 — transações, contas/cartões com fatura, fixos recorrentes, orçamentos, dashboard, investimentos e metas. Uso pessoal com potencial de virar SaaS.

Antes de implementar qualquer coisa, leia:

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

```bash
# Banco (Postgres 16 na porta 5433; Docker Desktop precisa estar rodando)
docker compose -f infra/docker-compose.yml up -d

# API — build + testes + cobertura (o que o CI roda)
cd api && ./mvnw verify

# Um teste só
./mvnw test -Dtest=AuthServiceTest
./mvnw test -Dtest=AuthServiceTest#shouldThrowEmailAlreadyUsed_whenEmailExists

# Rodar a API
./mvnw spring-boot:run

# Web — dev server em http://localhost:4200 (proxy /api → localhost:8080)
cd web && npm start

# Web — testes com cobertura (o que o CI roda; thresholds quebram o build)
npm run test:ci

# Um spec só
npm test -- --include='**/auth.service.spec.ts' --watch=false --browsers=ChromeHeadless

# Web — build de produção
npm run build:prod
```

**Gotcha de JDK:** o `java` default da máquina é 19; o projeto exige 21. Antes de qualquer comando Maven: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"` (PowerShell). Os testes de integração usam Testcontainers — Docker Desktop precisa estar de pé, senão o contexto falha com "find a Docker environment failed".

**URLs (context-path `/api`):** health em `http://localhost:8080/api/actuator/health`, Swagger em `http://localhost:8080/api/swagger-ui.html`. Endpoints: `/api/v1/...` (controllers usam `@RequestMapping("/v1/...")`; o prefixo vem de `server.servlet.context-path`).

## Qualidade — regras obrigatórias

- **Cobertura mínima de 90% de linha em testes unitários/integração — enforçada pelo JaCoCo na fase `verify`** (regra no `api/pom.xml`); build quebra abaixo disso. No frontend (a partir da sessão #3), thresholds no Karma: 90% statements/functions/lines, 80% branches. Toda feature nova entra com testes na mesma sessão — nunca como follow-up.
- Convenção de nomes de teste: `should<ComportamentoEsperado>_when<Condicao>` (ex.: `shouldReturn401_whenPasswordIsWrong`).
- Unit tests com Mockito para services; integração com `@SpringBootTest` + Testcontainers (Postgres real, nunca H2).
- Segurança: deny-by-default no `SecurityConfig` — todo endpoint novo é autenticado a menos que explicitamente liberado; `@Valid` em todo request DTO; segredos só via env var.
- CLAUDE.md e README devem ser atualizados no mesmo commit que muda comandos, endpoints ou arquitetura.

## Frontend (sessão #3)

Angular 20 standalone + signals + `inject()`; Tailwind v4 via `@tailwindcss/postcss` (`.postcssrc.json`); tema dark do protótipo em `src/styles.css` (variáveis CSS globais + classes `.panel`, `.card`, `.btn`, `.tag`...). Estrutura: `core/auth` (AuthService com signals, interceptor funcional, `authGuard`), `core/layout/shell` (sidebar), `features/<nome>` (uma pasta por página, lazy via `loadComponent`), `shared/`. API sempre por caminho relativo `/api/...` — em dev o `proxy.conf.json` encaminha para `localhost:8080` (não usar URL absoluta nem CORS). Token JWT em `localStorage` (`dindin.token`).

**Gotcha do Karma:** o `karma.conf.js` referenciado pelo builder `@angular/build:karma` **substitui** a config default em vez de mesclar — se recriar, use `ng generate config karma` e edite (senão os testes quebram com "describe is not defined"). Os thresholds de cobertura (90/80/90/90) vivem no `coverageReporter.check` desse arquivo.

## Domínio (sessões #5–#6)

- `/api/v1/accounts` e `/api/v1/categories`: CRUDs escopados por usuário (recurso alheio → 404); cartão exige `closingDay`/`dueDay`; categoria única por (user, nome, kind) → 409.
- `/api/v1/transactions`: `GET ?month=YYYY-MM` (obrigatório) `&accountId&categoryId&type&page&size` → `PageResponse`; POST/PUT/DELETE. `amount` sempre positivo (sinal vem do `type`); categoria deve ter kind coerente com o type; `INVOICE_ADJUSTMENT` é reservado (400 na API).
- **Vínculo de fatura:** compra em cartão no dia do fechamento ou depois → fatura do mês seguinte; `CardInvoiceService.getOrCreateInvoiceFor` cria a fatura OPEN do período na primeira compra (única por conta+mês; dias clampados ao fim do mês; vencimento ≤ fechamento cai no mês seguinte). Update de transação re-vincula; sair do cartão zera `invoice_id`.
- Delete de conta/categoria com transações → 409 (`DataIntegrityViolationException` no handler global).
- Filtros dinâmicos usam **JPA Specification** — não usar `(:param is null or ...)` em JPQL com UUID (quebra no Postgres/Hibernate 6).

## Auth (sessão #2)

JWT próprio HS256 (jjwt): `POST /api/v1/auth/register` (201) e `/login` retornam `{token, tokenType, expiresInSeconds}`; `GET /api/v1/auth/me` prova o token. `JwtAuthFilter` popula o SecurityContext com `AuthenticatedUser(id, email)` — controllers recebem via `@AuthenticationPrincipal`. Config: `JWT_SECRET` (≥32 bytes) e `JWT_EXPIRATION` (ISO-8601, default PT2H). Públicos: register/login, health, swagger. Erros padronizados pelo `GlobalExceptionHandler` (400 com `fieldErrors`, 401, 409, 500).

## Git workflow

Remote: `https://github.com/JokerFoxyy/DinDin.git`. Mesmo fluxo do ContratoIA:

1. **Ao iniciar qualquer feature/sessão, criar uma branch a partir de `develop`**: `git checkout develop && git checkout -b feature/<descricao-kebab>` (ex.: `feature/auth-jwt`, `feature/setup-frontend`). Nunca trabalhar direto na `main` ou `develop`.
2. Commitar na feature branch (commits pequenos e coerentes) e **sempre dar push para o GitHub**: `git push -u origin feature/<descricao-kebab>`. Trabalho não termina sem push — commit local só não conta.
3. Merge em `develop` via PR — o workflow `feature-pr.yml` cria o PR automaticamente no push da feature branch.
4. `develop → main` só via PR de release (criado automaticamente pelo `auto-pr.yml` no push da develop).

CI (`.github/workflows/`): `ci-api.yml` (mvnw verify com Testcontainers + JaCoCo 90%; imagem Docker → GHCR na main) e `ci-web.yml` (lint, build:prod, test:ci com thresholds) usam **filtros de path** — mudança só em `api/` não roda CI do front e vice-versa; ao editar um workflow, o próprio arquivo está nos paths. `security.yml`: CodeQL (Java e TS), Trivy fs (+ imagem na main), Dependency Review em PRs, cron semanal.
