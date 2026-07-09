# SDD — Sessão #2: Auth JWT

> **Data:** 2026-07-08 · **Pré-req:** #1 ✅ · **Plano-mestre:** `docs/PLANO-SDD.md`

## Objetivo

Registro e login com JWT próprio (HS256), Spring Security stateless protegendo tudo por padrão, e **JaCoCo com regra de 90% de cobertura de linha no `verify`** (novo padrão de qualidade do projeto, igual ContratoIA).

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Biblioteca JWT | jjwt 0.12.x (api/impl/jackson) | Madura, API fluente, HS256 |
| Segredo | `JWT_SECRET` env (default dev ≥ 32 bytes) | Nunca em código; HS256 exige 256 bits |
| Expiração | `JWT_EXPIRATION` (ISO-8601, default PT2H) | Configurável |
| Identidade no token | `sub` = UUID do usuário + claim `email` | UUID não enumerável |
| Context path | `server.servlet.context-path: /api` | Igual ContratoIA; casa com proxy Caddy `/api` no deploy |
| Endpoints | `POST /api/v1/auth/register` (201), `POST /api/v1/auth/login` (200), `GET /api/v1/auth/me` (autenticado) | `/me` serve de prova end-to-end do filtro |
| Público sem auth | `/v1/auth/register|login`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui*/**` | Default é negar (deny-by-default) |
| Senha | BCrypt (`PasswordEncoder`) | Padrão |
| Erros | `GlobalExceptionHandler`: 400 validação (erros por campo), 401 credenciais inválidas, 409 email duplicado, 500 genérico | Contrato de erro único desde o início |
| Cobertura | JaCoCo 0.8.13, regra BUNDLE LINE ≥ 0.90 no `verify` (exclui só a classe main) | Exigência do usuário; PR abaixo quebra build |

## Tasks

- **TASK-1 — Dependências e JaCoCo**: starter-security, jjwt, spring-security-test; plugin JaCoCo com check 90%.
- **TASK-2 — Domínio user**: entity `User` (UUID, email único, passwordHash, createdAt) + `UserRepository`.
- **TASK-3 — JWT + Security**: `JwtService` (gerar/validar), `JwtAuthFilter`, `SecurityConfig` stateless deny-by-default, context-path `/api`.
- **TASK-4 — Auth API**: `AuthService` (register/login), `AuthController` (register/login/me), DTOs com Bean Validation, `GlobalExceptionHandler`.
- **TASK-5 — Testes (≥90%)**: unit `JwtServiceTest` e `AuthServiceTest` (Mockito); integração `AuthFlowIntegrationTest` (Testcontainers + TestRestTemplate: register→login→me, 401 sem token, 409 duplicado, 400 validação).
- **TASK-6 — Verificação end-to-end**: `mvnw verify` verde com JaCoCo ≥90%; fluxo manual via API rodando contra o compose; docs atualizados (CLAUDE.md, README).

## Convenção de testes (novo padrão do projeto)

`should<ComportamentoEsperado>_when<Condicao>` — ex.: `shouldReturn401_whenPasswordIsWrong`. Mockar dependências externas; testes rápidos e isolados.

## Status

- [x] TASK-1 — Dependências e JaCoCo
- [x] TASK-2 — Domínio user
- [x] TASK-3 — JWT + Security
- [x] TASK-4 — Auth API
- [x] TASK-5 — Testes (≥90%)
- [x] TASK-6 — Verificação end-to-end

**Resultado da verificação (2026-07-08):**
- `mvnw verify`: BUILD SUCCESS — 19 testes (5 JwtService, 5 AuthService, 7 integração, 2 base), **cobertura de linha 97,3%** (108/111), JaCoCo check ≥90% ativo
- Fluxo manual contra o compose: register 201 (token 7200s) → login → `GET /api/v1/auth/me` retorna id/email; sem token → 401; senha errada → 401
- Health UP em `/api/actuator/health` (context-path novo)
