# SDD — Sessão #21: Deploy AWS

> **Data:** 2026-07-18 · **Pré-req:** #4 + MVP estável · **Branch:** `feature/deploy-aws` · **Plano-mestre:** `docs/PLANO-SDD.md`
> **Fase 3 — Qualidade de vida. Fecha a fase.**

## Objetivo

Colocar o DinDin acessível na internet de verdade, com HTTPS, backup automatizado e deploy repetível — sem depender de passos manuais em cada release.

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Instância | **Lightsail US$5/mês** (Ubuntu) | decisão do usuário nesta sessão — preço fixo previsível, IP estático incluso, menos passos de rede manual que EC2. **Consequência**: Lightsail roda em x86_64, não ARM — o plano original mencionava "Dockerfiles ARM64" pensando na alternativa EC2 Graviton; como Lightsail foi a escolha, as imagens são buildadas para `linux/amd64` (arquitetura padrão do runner do GitHub Actions), não ARM. |
| Onde builda a imagem | **CI (GitHub Actions)**, nunca na instância | a instância tem 1GB de RAM — buildar Maven+Angular ali travaria ou daria OOM. `ci-api.yml` já publicava a imagem da API no GHCR desde a sessão #4; replicado o mesmo padrão (`docker` job) em `ci-web.yml` para a imagem do frontend. |
| Frontend + proxy | **Caddy** servindo o build estático do Angular E fazendo reverse proxy de `/api/*` pro container da API | um único processo/container resolve TLS automático (Let's Encrypt) e serve os dois papéis, sem precisar de Nginx + certbot configurados à mão. |
| TLS | Caddy com `{$DOMAIN}` e `{$ACME_EMAIL}` parametrizados por env — domínio comprado pelo usuário nesta sessão (decisão explícita, ver "Decisões do usuário" abaixo) | sem domínio não há certificado automático válido (Let's Encrypt exige um domínio público). |
| Deploy | GitHub Actions `workflow_dispatch` (manual) → SSH na instância → `git pull` + `docker compose pull` + `up -d` | deploy em produção é deliberado, não a cada merge; a instância só puxa imagens prontas do GHCR, não builda nada. |
| Backup | `pg_dump` via cron/systemd no host (não em container) → gzip → S3, lifecycle de 30 dias no bucket | script simples, sem dependência de ferramenta externa de backup; lifecycle do próprio S3 evita custo crescendo indefinidamente. |
| Swap | 2GB via `setup-host.sh` (script de setup único) | instância de 1GB sofre com picos de memória (migrations do Flyway, restart da JVM); swap evita OOM-kill do container em vez de crashar. |
| Secrets | **Nunca manipulados por mim** — usuário roda `gh secret set` no próprio terminal para `DEPLOY_HOST`/`DEPLOY_USER`/`DEPLOY_SSH_KEY` | chave SSH privada e credenciais de produção não devem transitar por uma sessão de IA, mesmo que o usuário autorize. |

## Decisões do usuário (perguntadas nesta sessão)

- **Domínio**: usuário ainda não tinha um — decidiu **comprar um domínio agora**, antes de eu escrever o Caddyfile, para já sair com TLS automático configurado desde o início (em vez de IP puro sem TLS como alternativa mais rápida).
- **Instância**: Lightsail US$5/mês (recomendado), não EC2.

## Tasks

- **TASK-1** — `web/Dockerfile` (build Angular + Caddy servindo estático) e `web/Caddyfile` (proxy `/api/*`, SPA fallback, TLS automático via `{$DOMAIN}`/`{$ACME_EMAIL}`); `api/Dockerfile` já existia (sessão #4), sem mudanças.
- **TASK-2** — `infra/docker-compose.prod.yml` (postgres + api + web/Caddy, todos com `restart: unless-stopped`, imagens do GHCR via env) + `infra/.env.prod.example`.
- **TASK-3** — `infra/scripts/backup.sh` (pg_dump → S3), `infra/scripts/configure-s3-lifecycle.sh` (setup único do bucket) e `infra/scripts/setup-host.sh` (Docker + swap 2GB + clone do repo, setup único da instância).
- **TASK-4** — `.github/workflows/deploy.yml` (SSH manual) + job `docker` novo em `ci-web.yml` (publica `ghcr.io/jokerfoxyy/dindin-web`, espelhando o job já existente em `ci-api.yml`).
- **TASK-5** — Verificação: build local das duas imagens + smoke test do `docker-compose.prod.yml` de ponta a ponta (rede interna, proxy, fallback SPA) com `DOMAIN=:80` (bypassa TLS automático do Caddy só para o teste local, já que não há domínio público resolvendo para esta máquina).

## Status

- [x] TASK-1 — Dockerfiles + Caddyfile
- [x] TASK-2 — docker-compose de produção
- [x] TASK-3 — Scripts de backup e setup do host
- [x] TASK-4 — CI/CD (build+push da imagem web, workflow de deploy)
- [x] TASK-5 — Verificação local (smoke test)

**Resultado da verificação (2026-07-18):**
- `docker build` de `api/Dockerfile` e `web/Dockerfile`: ambos com sucesso.
- `caddy validate --config Caddyfile` com `DOMAIN`/`ACME_EMAIL` definidos: `Valid configuration`.
- Smoke test do `docker-compose.prod.yml` local (imagens buildadas localmente, `DOMAIN=:80`):
  - `GET /api/actuator/health` → `200 {"status":"UP"}` (proxy Caddy → container `api` → Postgres, healthcheck ok).
  - `GET /` → `200`, HTML do build Angular servido pelo Caddy.
  - `GET /dashboard` (rota client-side) → `200`, `try_files` cai no `index.html` (SPA fallback correto).
- Ambiente de teste (containers, volumes, imagens `-test`, `.env.smoketest`) removido ao final — nada disso foi commitado.

**O que fica pendente de verificação end-to-end real em produção** (depende de ações que só o usuário pode fazer — ver `infra/README.md` para o passo a passo completo):
1. Comprar o domínio e apontar o registro A para o IP estático da instância.
2. Criar a instância Lightsail e rodar `infra/scripts/setup-host.sh`.
3. Preencher `infra/.env` com segredos reais e subir a stack pela primeira vez.
4. Criar o bucket S3 e rodar `configure-s3-lifecycle.sh`.
5. Configurar os secrets do GitHub (`DEPLOY_HOST`/`DEPLOY_USER`/`DEPLOY_SSH_KEY`) e disparar o workflow **Deploy** manualmente.
6. Confirmar HTTPS válido de verdade (certificado emitido pelo Let's Encrypt) e o backup rodando contra o bucket real.

Assim que o usuário completar esses passos, uma verificação end-to-end real em produção deve ser feita e registrada aqui (ou numa sessão de acompanhamento).
