# SDD — Sessão #23 (parte 2): Rebranding Guaranin → Poupito

> **Data:** 2026-07-22 · **Branch:** `feature/rebrand-poupito` · **Plano-mestre:** `docs/PLANO-SDD.md`
> **Fase 4 — parte "nome" da sessão #23 (Identidade visual).** Continuação de `docs/session-23-rebrand-guaranin/SDD.md`.

## Objetivo

Trocar o nome do app de "Guaranin" para "**Poupito**" — nome final, escolhido depois que "Guaranin" se mostrou problemático (soava a guaraná/guarani, não comunicava dinheiro). "Poupito" vem de "poupar", comunica economizar de forma leve, e tem o `.com` livre.

## Decisões e contexto

| Item | Decisão | Motivo |
|---|---|---|
| Nome | "Poupito" | De "poupar" + diminutivo; comunica economizar; `poupito.com` livre (confirmado via RDAP da Verisign). |
| Escopo | Rename **completo** (pacote Java, repositório GitHub, cookies, infra, docs vivos), igual ao rename anterior | decisão do usuário — mesma linha do rename DinDin→Guaranin; aceitou o escopo/risco maior. |
| Domínio | `poupito.com` a registrar pelo usuário (Cloudflare); `poupito.com.br` já está com terceiros mas **sem site ativo** (não resolve) — não bloqueia | `.com` é o que importa pra marca; `.com.br` pode ser buscado depois se o app crescer. Verificado que nenhum outro app financeiro óbvio usa o nome (busca do usuário; INPI pendente). |
| Docs históricos | SDDs de sessões passadas (`docs/session-NN-*`), incluindo `session-23-rebrand-guaranin`, **mantidos como estão** | registro do que existia na época; só os docs vivos são atualizados. |

## Tasks executadas

- **Repo GitHub**: `JokerFoxyy/Guaranin` → `JokerFoxyy/Poupito` (redirect preservado, remote local atualizado, PRs abertos preservados).
- **Backend**: pacote `com.guaranin.api` → `com.poupito.api` (main+test, `git mv` + sed em package/imports); `pom.xml` (groupId/description); `application.yml` (nome da app, defaults de DB/JWT dev); cookies `guaranin_at/rt` → `poupito_at/rt`; filename de export LGPD.
- **Frontend**: manifest PWA (nome/short_name), `index.html` title, logo (login + sidebar: "Poup"+"ito"), chaves de `localStorage`, emails de teste.
- **Infra**: `docker-compose.yml`/`.prod.yml` (nomes de container/DB/imagens GHCR `poupito-api`/`-web`), `.env.prod.example`, scripts (`/opt/guaranin` → `/opt/poupito`, URL do repo), `deploy.yml`, `security.yml` (tag da imagem no Trivy), `infra/README.md`.
- **Docs vivos**: `CLAUDE.md`, `README.md`, `docs/PLANO-SDD.md`, `docs/security/lgpd.md`. Pasta local do threat model renomeada `D:\Docs\Guaranin` → `D:\Docs\Poupito`.

## Verificação (2026-07-22)

- API: `mvnw verify` — **259 testes**, JaCoCo ≥90%, BUILD SUCCESS.
- Web: `npm run test:ci` — **213 testes**, cobertura 96,66/84,86/93,16/96,59%; `build:prod` ok.
- Visual: dev server em produção, título da aba e logo mostrando "Poupito" no browser.

## Pendências (continuam da parte 1 — a parte "identidade visual" de verdade)

Logo com símbolo próprio, paleta de cores oficial documentada, ícones reais do manifest PWA e favicon — usuário está definindo slogan e cores em paralelo.
