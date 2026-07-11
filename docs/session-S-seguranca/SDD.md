# SDD — Sessão S: Segurança (STRIDE + LGPD + Confidencialidade)

> **Data:** 2026-07-10 · **Inserida a pedido do usuário** entre #7 e #8 · **Branch:** `feature/seguranca-stride-lgpd`
> **Escopo escolhido:** completo ("tudo, incluindo o pesado") — documentação + hardening + reescrita de auth para cookie httpOnly + refresh/revogação + endpoints LGPD.

## Objetivo

Elevar a postura de segurança do DinDin ao nível esperado de um app financeiro: modelar ameaças (STRIDE), mapear conformidade LGPD, e implementar as mitigações — incluindo mudar o modelo de autenticação de "JWT no localStorage" para "cookies httpOnly + refresh token revogável", e criar os direitos do titular (exportação e exclusão de conta).

## Migrations

- **V4** — `refresh_tokens` (esta sessão). ⚠️ Consequência: a sessão #8 (fixos) passa a usar **V5** (o SDD da #6 mencionava V4 para `recurring_transactions`).

## Partes e Tasks

### Parte A — Documentação (deliverables)
- **TASK-A1** — `docs/security/threat-model-stride.md`: DFD textual, ativos, fronteiras de confiança, tabela STRIDE por categoria com ameaça → mitigação (atual/planejada) → status.
- **TASK-A2** — `docs/security/lgpd.md`: inventário de dados pessoais, base legal, direitos do titular e onde são atendidos, retenção, medidas de confidencialidade, incidentes.

### Parte B — Hardening seguro (backend)
- **TASK-B1** — Fail-fast de segredos: em profile `prod`, recusar subir se `JWT_SECRET`/`DB_PASSWORD` forem os defaults de dev; exigir segredo ≥ 32 bytes.
- **TASK-B2** — Rate limiting em `/auth/login` e `/auth/register` (janela fixa por IP+email, in-memory; nota para Redis em multi-instância) → 429.
- **TASK-B3** — BCrypt strength 12; política de senha mais forte no registro (mín. 10, exige variedade).
- **TASK-B4** — Security headers (X-Content-Type-Options, X-Frame-Options DENY, Referrer-Policy, HSTS em prod) e Swagger fechado em prod.
- **TASK-B5** — Revisão de logs: garantir que senha/token/refresh nunca são logados.

### Parte C — Auth cookie httpOnly + refresh + revogação
- **TASK-C1** — Access token curto (15 min) em cookie `dindin_at` httpOnly+SameSite=Strict (Secure em prod); refresh token opaco (256 bits, hash SHA-256 no banco, 30 dias, rotacionado, revogável) em cookie `dindin_rt` (path `/api/v1/auth`).
- **TASK-C2** — Endpoints: register/login setam cookies e retornam só `{id,email}` (sem token no corpo); `POST /auth/refresh` rotaciona; `POST /auth/logout` revoga no banco e limpa cookies. Filtro lê token do cookie (fallback Authorization).
- **TASK-C3** — Frontend: remover token do `localStorage` (flag booleana não-sensível só para roteamento), interceptor com `withCredentials` e auto-refresh em 401, ajustar service/guard/testes.

### Parte D — Direitos do titular (LGPD)
- **TASK-D1** — `GET /v1/account/export`: devolve todos os dados do usuário (JSON) — portabilidade.
- **TASK-D2** — `DELETE /v1/account`: exclui usuário e todos os dados vinculados numa transação — direito de eliminação; tela de confirmação no front.

### Verificação
- **TASK-V** — `mvnw verify` + `npm run test:ci` verdes (cobertura ≥ thresholds); e2e no browser: login seta cookie httpOnly (não visível ao JS), refresh mantém sessão, logout revoga, exportação baixa JSON, exclusão apaga tudo; PR com CI verde.

## Decisões e trade-offs

- **CSRF:** mantido `csrf.disable()`; a defesa é `SameSite=Strict` (cookie não vai em requisição cross-site) + API JSON same-origin. Documentado no threat model.
- **Enumeração no registro:** o 409 "email já cadastrado" é mantido (usabilidade) e **mitigado por rate limiting**, não removido — risco aceito e registrado.
- **Cookie Secure:** `false` em dev (http localhost), `true` em prod — via `app.security.cookie-secure`.
- **Rate limiter in-memory:** suficiente para deploy single-instance (Lightsail/EC2 da #21); multi-instância exigirá store compartilhado.

## Status

- [x] A1 threat model · [x] A2 lgpd
- [x] B1 segredos · [x] B2 rate limit · [x] B3 bcrypt/senha · [x] B4 headers · [x] B5 logs
- [x] C1 tokens · [x] C2 endpoints · [x] C3 frontend
- [x] D1 export · [x] D2 delete
- [x] V verificação

**Resultado da verificação (2026-07-11):**
- API: `mvnw verify` — **101 testes** (novos: RefreshTokenService, LoginRateLimiter, SecretsValidator, UserDataService, LgpdFlow + AuthFlow com refresh/logout/rate-limit/cookies), JaCoCo ≥90% OK
- Web: `npm run test:ci` — **77 testes**, cobertura 98,2/86,2/94,2/98,0
- E2E no browser (stack completo):
  - Registro → `document.cookie` **vazio** (tokens httpOnly, invisíveis ao JS); só a flag `dindin.authed` no localStorage
  - Chamadas de API autenticadas por cookie (`/me` 200, exportação 200 com Content-Disposition attachment)
  - Logout → `/me` e `/refresh` retornam 401 (refresh token revogado no servidor)
  - Exclusão de conta: botão travado até digitar "EXCLUIR"; após excluir, `/me` 401 (usuário e dados apagados)
- Nota operacional: Docker Desktop caiu no meio de um build (Testcontainers → "Could not find a valid Docker environment"); os testes em si passam com Docker de pé.
