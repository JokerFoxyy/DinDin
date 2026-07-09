# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## O que é este projeto

DinDin (DinDin): app de gestão financeira pessoal que substitui a Planilha_Gastos_2026 — transações, contas/cartões com fatura, fixos recorrentes, orçamentos, dashboard, investimentos e metas. Uso pessoal com potencial de virar SaaS.

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
```

**Gotcha de JDK:** o `java` default da máquina é 19; o projeto exige 21. Antes de qualquer comando Maven: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"` (PowerShell). Os testes de integração usam Testcontainers — Docker Desktop precisa estar de pé, senão o contexto falha com "find a Docker environment failed".

**URLs (context-path `/api`):** health em `http://localhost:8080/api/actuator/health`, Swagger em `http://localhost:8080/api/swagger-ui.html`. Endpoints: `/api/v1/...` (controllers usam `@RequestMapping("/v1/...")`; o prefixo vem de `server.servlet.context-path`).

## Qualidade — regras obrigatórias

- **Cobertura mínima de 90% de linha em testes unitários/integração — enforçada pelo JaCoCo na fase `verify`** (regra no `api/pom.xml`); build quebra abaixo disso. No frontend (a partir da sessão #3), thresholds no Karma: 90% statements/functions/lines, 80% branches. Toda feature nova entra com testes na mesma sessão — nunca como follow-up.
- Convenção de nomes de teste: `should<ComportamentoEsperado>_when<Condicao>` (ex.: `shouldReturn401_whenPasswordIsWrong`).
- Unit tests com Mockito para services; integração com `@SpringBootTest` + Testcontainers (Postgres real, nunca H2).
- Segurança: deny-by-default no `SecurityConfig` — todo endpoint novo é autenticado a menos que explicitamente liberado; `@Valid` em todo request DTO; segredos só via env var.
- CLAUDE.md e README devem ser atualizados no mesmo commit que muda comandos, endpoints ou arquitetura.

## Auth (sessão #2)

JWT próprio HS256 (jjwt): `POST /api/v1/auth/register` (201) e `/login` retornam `{token, tokenType, expiresInSeconds}`; `GET /api/v1/auth/me` prova o token. `JwtAuthFilter` popula o SecurityContext com `AuthenticatedUser(id, email)` — controllers recebem via `@AuthenticationPrincipal`. Config: `JWT_SECRET` (≥32 bytes) e `JWT_EXPIRATION` (ISO-8601, default PT2H). Públicos: register/login, health, swagger. Erros padronizados pelo `GlobalExceptionHandler` (400 com `fieldErrors`, 401, 409, 500).

## Git workflow

Remote: `https://github.com/JokerFoxyy/DinDin.git`. Mesmo fluxo do ContratoIA:

1. **Ao iniciar qualquer feature/sessão, criar uma branch a partir de `develop`**: `git checkout develop && git checkout -b feature/<descricao-kebab>` (ex.: `feature/auth-jwt`, `feature/setup-frontend`). Nunca trabalhar direto na `main` ou `develop`.
2. Commitar na feature branch (commits pequenos e coerentes) e **sempre dar push para o GitHub**: `git push -u origin feature/<descricao-kebab>`. Trabalho não termina sem push — commit local só não conta.
3. Merge em `develop` via PR (a partir da sessão #4, um Action cria o PR automaticamente no push da feature branch; até lá, criar com `gh pr create` ou merge local + push).
4. `develop → main` também só via PR de release.

Enquanto a branch protection não estiver configurada no GitHub (pré-req da sessão #4), a disciplina é a mesma — o hábito não muda.
