# SDD — Sessão #3: Setup Frontend Angular

> **Data:** 2026-07-09 · **Pré-req:** #2 ✅ · **Branch:** `feature/setup-frontend` · **Plano-mestre:** `docs/PLANO-SDD.md`

## Objetivo

App Angular 20 em `web/` com o tema dark do protótipo, shell de navegação (sidebar + topbar), login/registro funcionando contra a API real (`/api/v1/auth/*`) e cobertura ≥ thresholds no Karma.

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Angular | 20.x, standalone components + signals, SCSS, sem SSR | SPA atrás de Caddy no deploy; padrão do usuário (ContratoIA) |
| Tailwind | v4 via `@tailwindcss/postcss` | Utilitários + variáveis CSS do protótipo para o tema |
| Tema | Variáveis do protótipo (`--bg:#0d1117`, `--card:#161b22`, `--accent:#4f8ef7`...) em `styles.scss` | Fidelidade visual ao `prototipo-dashboard.html` |
| API | `apiUrl: '/api'` + proxy dev (`proxy.conf.json` → `localhost:8080`) | Evita CORS no backend; espelha o Caddy de produção (proxy `/api`) |
| Token | `localStorage` + interceptor funcional (Bearer) | Simples; interceptor pula anexo quando não autenticado |
| Estado auth | `AuthService` com signals (`isAuthenticated`, `currentUser`) | Estilo do ContratoIA frontend (inject() + signals) |
| Guard | `authGuard` funcional → redireciona `/login` | Deny-by-default: toda rota do shell é guardada |
| Rotas | `/login` público; shell guardado com `dashboard`, `transacoes`, `investimentos`, `metas`, `fixos`, `configuracoes` (placeholders) | Estrutura das sessões futuras já roteada |
| Cobertura | `karma.conf.js` com thresholds **90% statements/functions/lines, 80% branches** (build falha abaixo) | Regra do CLAUDE.md |

## Tasks

- **TASK-1 — Projeto Angular**: `ng new` em `web/`, Tailwind v4, tema dark, proxy dev.
- **TASK-2 — Core auth**: `AuthService` (register/login/me/logout), interceptor JWT, `authGuard`, environments.
- **TASK-3 — Shell + telas**: layout sidebar/topbar fiel ao protótipo, tela login/registro, placeholders das features, rotas.
- **TASK-4 — Testes ≥ thresholds**: specs de service/interceptor/guard/componentes; `karma.conf.js` com enforcement.
- **TASK-5 — Verificação end-to-end**: `npm test` e `npm run build` verdes; fluxo real no browser (registro → login → shell → logout) contra a API + Postgres.

## Status

- [x] TASK-1 — Projeto Angular
- [x] TASK-2 — Core auth
- [x] TASK-3 — Shell + telas
- [x] TASK-4 — Testes ≥ thresholds
- [x] TASK-5 — Verificação end-to-end

**Resultado da verificação (2026-07-09):**
- `npm run test:ci`: 30/30 testes, cobertura **100% statements · 90% branches · 100% functions · 100% lines** (thresholds ativos no `karma.conf.js`)
- `npm run build:prod`: OK, lazy chunk por feature, dentro do budget
- E2E real no browser (dev server + proxy + API + Postgres): registro → token no localStorage → `/dashboard` com shell e email carregado via `/me`; logout limpa token e volta a `/login`; acesso direto a `/dashboard` deslogado → guard redireciona; login de novo → entra. Tema conferido via computed styles (`#0d1117`/`#161b22`/`#e6edf3`)
- Gotcha registrado: o `karma.conf.js` do builder `@angular/build:karma` **substitui** (não mescla) a config — gerar com `ng generate config karma` e editar, nunca escrever do zero
