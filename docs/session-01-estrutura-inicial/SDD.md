# SDD — Sessão #1: Estrutura Inicial + Backend Base

> **Data:** 2026-07-07 · **Pré-req:** nenhum · **Plano-mestre:** `docs/PLANO-SDD.md`

## Objetivo

Monorepo funcional com Postgres em Docker e API Spring Boot subindo com migration V1 aplicada, healthcheck e Swagger UI. Ao final: `docker compose up` + `mvnw spring-boot:run` = API viva.

## Contexto do ambiente

- JDK 21 em `C:\Program Files\Java\jdk-21` (default do sistema é 19 → builds usam `JAVA_HOME` explícito)
- Maven 3.9.9, Docker 29.x, Node 20 (Node só será usado na sessão #3)

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Build | Maven + wrapper (`mvnw`) | Padrão da stack do usuário (ContratoIA) |
| Spring Boot | 3.5.x (última patch) | Já endurecido no ContratoIA (3.5.14) |
| GroupId/pacote | `com.financeia.api` | Simples, monorepo `api/` |
| Postgres | 16-alpine, porta 5433 no host | 5433 evita conflito com Postgres local/ContratoIA |
| Flyway V1 | apenas `users` | Auth completa é sessão #2; V1 já deixa a base |
| Security | **fora** desta sessão | Sem Spring Security agora — endpoints abertos até #2 |
| springdoc | 2.x (`/swagger-ui.html`) | Contrato documentado desde o dia 1 |
| Virtual threads | `spring.threads.virtual.enabled=true` | Java 21, conforme spec |

## Tasks

- **TASK-1 — Estrutura do monorepo**: pastas `api/`, `web/` (placeholder `.gitkeep`), `infra/`, `docs/`; `.gitignore` raiz; `README.md` breve.
- **TASK-2 — Docker Compose (Postgres)**: `infra/docker-compose.yml` com postgres:16-alpine, volume nomeado, healthcheck, porta 5433, credenciais dev via env com default.
- **TASK-3 — Projeto Spring Boot**: `api/` com pom (web, data-jpa, postgres, flyway, validation, actuator, springdoc), wrapper mvnw, `application.yml` (datasource localhost:5433, JPA `ddl-auto: validate`, virtual threads), classe main, migration `V1__create_users.sql`.
- **TASK-4 — Verificação end-to-end**: `docker compose up -d` → banco healthy; `mvnw verify` compila e testes passam; API sobe; `GET /actuator/health` = UP; Swagger UI responde; tabela `users` + `flyway_schema_history` existem no banco.

## Critérios de aceite

1. `docker compose -f infra/docker-compose.yml up -d` deixa o Postgres healthy.
2. `mvnw spring-boot:run` (JAVA_HOME=jdk-21) sobe sem erro e aplica V1.
3. `/actuator/health` retorna `{"status":"UP"}`.
4. `/swagger-ui.html` carrega.
5. Commit único na branch `main` com a estrutura.

## Status

- [x] TASK-1 — Estrutura do monorepo
- [x] TASK-2 — Docker Compose (Postgres)
- [x] TASK-3 — Projeto Spring Boot base
- [x] TASK-4 — Verificação end-to-end

**Resultado da verificação (2026-07-07):**
- `mvnw verify` (JDK 21): BUILD SUCCESS — 2 testes passando (context + Flyway cria `users`) via Testcontainers
- Postgres do compose: healthy na porta 5433; tabelas `users` e `flyway_schema_history` criadas (V1 success=t)
- `GET /actuator/health` → `{"status":"UP"}` · `/swagger-ui.html` → HTTP 200
- Nota: Spring Boot fixado em **3.5.14** (Initializr gerou 4.1.0; pom reescrito para alinhar com a decisão do SDD)
