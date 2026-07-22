# SDD — Sessão #23: Rebranding para Guaranin

> **Data:** 2026-07-19 · **Pré-req:** nenhum · **Branch:** `feature/rebrand-guaranin` · **Plano-mestre:** `docs/PLANO-SDD.md`
> **Fase 4 — consome o slot da sessão #23 (Identidade visual), planejada no roadmap.**

## Objetivo

Renomear o produto de "DinDin" para **"Guaranin"** — nome, marca, domínio próprio e repositório — antes do deploy em produção (sessão #21), já que o usuário comprou o domínio `guaranin` e decidiu que o app deve se chamar assim.

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Escopo do rename | **Tudo**: pacote Java, repositório GitHub, marca visível — não só UI | decisão explícita do usuário, perguntada via clarificação: "só marca/UI" vs. "tudo, incluindo pacote Java e repositório" — usuário escolheu o segundo, aceitando o escopo/risco maior. |
| Pacote Java | `com.dindin.api` → `com.guaranin.api` (main + test, ~175 arquivos movidos) | consistência total do código com a marca nova; feito via `git mv` (preserva histórico) + `sed` em massa nos `.java` para packages/imports. |
| Repositório GitHub | `JokerFoxyy/DinDin` → `JokerFoxyy/Guaranin` (via `gh repo rename`) | GitHub mantém redirect do nome antigo e preserva PRs/issues abertos — confirmado que os PRs #43/#44 sobreviveram ao rename antes de prosseguir. |
| Docs históricos | `docs/session-NN-*/SDD.md` **não foram alterados** | são registros datados do que existia/foi decidido na época (sob o nome antigo) — reescrevê-los seria revisionismo. Só docs "vivos" (CLAUDE.md, README.md, docs/PLANO-SDD.md, docs/security/*) foram atualizados. |
| Cookies de auth | `dindin_at`/`dindin_rt` → `guaranin_at`/`guaranin_rt` | consistência total; sessões antigas ficam invalidadas (aceitável, produto ainda não está em produção). |
| DB/infra dev | Nomes default de banco/usuário/senha em `application.yml` e `infra/docker-compose.yml` (dev) também renomeados | consistência; são apenas defaults de desenvolvimento local, sem impacto em dados reais. |
| Logo textual | Split "Din"+"Din" (cores diferentes via `<span>`) virou "Guara"+"nin" | mantém o mesmo padrão visual (duas cores) do CSS existente (`.logo` / `.logo span`), sem precisar mexer no CSS. |

## Tasks

- **TASK-1** — Renomear pacote Java `com.dindin.api` → `com.guaranin.api` (diretórios + package/import statements); `pom.xml` (groupId, description); `application.yml` (nome da app, defaults de DB/JWT secret dev); cookies de auth; filename de export LGPD.
- **TASK-2** — Rebrand user-facing: manifest PWA, `index.html`, logo (login + shell), chaves de `localStorage`, `README.md`, `CLAUDE.md`, `docs/PLANO-SDD.md`, `docs/security/*`.
- **TASK-3** — Renomear repositório GitHub (`gh repo rename`) + atualizar remote local; confirmar PRs abertos preservados.
- **TASK-4** — Atualizar infra de deploy (branch `feature/deploy-aws`, ainda não mergeada): merge deste branch nela + rename de imagens GHCR, nomes de container, paths `/opt/guaranin`, URLs.
- **TASK-5** — Testes: `mvn verify` (API) + `npm run test:ci` (web) completos após o rename; verificação visual no browser (build de produção, título da aba e logo mostrando "Guaranin").

## Status

- [x] TASK-1 — Pacote Java + config backend
- [x] TASK-2 — Rebrand user-facing + docs
- [x] TASK-3 — Repositório GitHub
- [x] TASK-4 — Infra de deploy (branch feature/deploy-aws, PR #44 atualizado)
- [x] TASK-5 — Testes + verificação

**Resultado da verificação (2026-07-19):**
- API: `mvnw verify` — **259 testes**, JaCoCo ≥90%, BUILD SUCCESS, com o pacote já `com.guaranin.api` em todo o código.
- Web: `npm run test:ci` — **213 testes**, cobertura 96,66/84,86/93,16/96,59% (acima de 90/80/90/90).
- `npm run build:prod` + servido no browser: título da aba "Guaranin", logo "Guara**nin**" na tela de login e na sidebar, manifest PWA com nome/tema corretos.
- Confirmado via `git diff --cached --stat` que o rename tocou exatamente os arquivos com referências a "dindin"/"DinDin" (197 arquivos, 711 inserções/711 remoções — simétrico, só substituição de texto) — nenhum arquivo alheio foi afetado.
- Repositório GitHub renomeado (`gh repo rename`), remote local atualizado, PRs #43 e #44 confirmados intactos após o rename.
- Branch `feature/deploy-aws` (PR #44) recebeu merge deste branch + rename adicional específico da infra de deploy (imagens GHCR, containers, paths).

**Pendências que ficam para o usuário:**
- Domínio "guaranin" já comprado — falta apontar o DNS pro IP da instância Lightsail quando ela existir (sessão #21 ainda não provisionada de verdade).
- Ícones do manifest PWA continuam sendo o placeholder do Angular (pendência já registrada na sessão #20/#23 original do roadmap) — logo/paleta oficial de verdade ainda não foi desenhada.
