# SDD — Sessão #4: Esteira CI/CD (espelho do ContratoIA)

> **Data do plano:** 2026-07-08 · **Pré-req:** #1–#3 + repo publicado no GitHub · **Plano-mestre:** `docs/PLANO-SDD.md`
> **Referência:** workflows de `contrato-ia-backend/.github/workflows/` e `contrato-ia-frontend/.github/workflows/`

## Objetivo

Replicar a esteira do ContratoIA neste monorepo: CI com cobertura mínima 90%, security scanning (CodeQL + Trivy + Dependency Review), auto-PRs por branch e imagem Docker no GHCR. Diferença estrutural: **ContratoIA tem 3 repos; FinanceIA é monorepo** → um único `.github/workflows/` com filtros de path (`api/**`, `web/**`) para não rodar build de front quando só o back mudou.

## Pré-requisito manual (USUÁRIO)

- ~~Criar repo no GitHub e push da `main`~~ ✅ feito em 2026-07-08 (`JokerFoxyy/FinanceIA`, main + develop publicadas)
- Branch protection em `main` e `develop`: só merge via PR aprovado (igual ContratoIA — **nunca push direto**). Config no GitHub: Settings → Branches → Add rule (**pendente, USUÁRIO**).

## Workflows planejados

### TASK-1 — `ci-api.yml` (CI Backend)
Espelho do `ci.yml` do contrato-ia-backend, com adaptações:
- Triggers: push em `main`, `develop`, `feature/**` + PRs para `main`/`develop`, com `paths: [api/**]`.
- `concurrency` com cancel-in-progress.
- Job `build-and-test` (ubuntu, JDK 21 temurin, cache maven): `mvn clean verify -B` em `api/`.
  - **Sem service container de Postgres**: FinanceIA usa Testcontainers (o runner do GH Actions tem Docker) — mais fiel ao ambiente local que o service container do ContratoIA.
  - Upload de artifacts: `surefire-reports/` e `site/jacoco/` (retenção 7 dias) + coverage summary no `$GITHUB_STEP_SUMMARY`.
  - **Cobertura 90% é enforçada pelo próprio JaCoCo no `verify`** (regra no pom desde a sessão #2) — build vermelho abaixo disso.
- Job `docker` (só push na `main`): build da imagem `api/Dockerfile` e push para GHCR com tags `sha`, `latest`, `datetime` (metadata-action), cache GHA.

### TASK-2 — `ci-web.yml` (CI Frontend)
Espelho do `ci.yml` do contrato-ia-frontend:
- Mesmos triggers com `paths: [web/**]`.
- Job `lint-and-build`: Node 20 + cache npm, `npm ci`, `npm run lint --if-present`, `npm run build:prod`; artifact do `dist/` na main.
- Job `test`: `npm test -- --watch=false --browsers=ChromeHeadless --code-coverage` + upload do coverage.
- Thresholds no `karma.conf.js` (**90% statements/functions/lines, 80% branches**) — build falha abaixo.

### TASK-3 — `security.yml`
Espelho do security.yml do ContratoIA, unificado para o monorepo:
- Triggers: push/PR em `main`/`develop` + cron semanal (segunda 6h UTC) para novas CVEs.
- Job `codeql-java`: CodeQL `java-kotlin`, queries `security-and-quality`, `mvn compile -DskipTests`.
- Job `codeql-js`: CodeQL `javascript-typescript` (equivalente ao scan do repo frontend).
- Job `dependency-check`: Trivy fs scan (CRITICAL,HIGH) → SARIF → GitHub Security.
- Job `container-scan` (só main): Trivy na imagem Docker da API.
- Job `dependency-review` (só PRs): `dependency-review-action`, `fail-on-severity: critical`, comenta no PR.

### TASK-4 — `feature-pr.yml` + `auto-pr.yml`
Cópia direta do ContratoIA (mudando apenas nomes):
- `feature-pr.yml`: push em `feature/**` → cria/atualiza PR `feature/* → develop` com título derivado da branch e lista de commits.
- `auto-pr.yml`: push em `develop` → cria/atualiza PR de release `develop → main` com checklist.

### TASK-5 — Verificação end-to-end
Push em branch `feature/ci-cd` → conferir: PR automático criado, CI api verde (com JaCoCo ≥90%), CI web verde, security scan sem CRITICAL, path filters funcionando (commit só em `web/` não dispara CI da api).

## Fora de escopo desta sessão

- `deploy.yml` (CodeDeploy/SSH) → sessão #21, junto com a infra AWS.
- Fluxo de trabalho até lá: enquanto não houver remote GitHub, commits locais na `main`; a partir desta sessão, **feature branches + auto-PR igual ContratoIA**.

## Status

- [ ] TASK-1 — ci-api.yml
- [ ] TASK-2 — ci-web.yml
- [ ] TASK-3 — security.yml
- [ ] TASK-4 — feature-pr.yml + auto-pr.yml
- [ ] TASK-5 — Verificação end-to-end
