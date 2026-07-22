# Poupito — Plano SDD de Implementação

> **Fonte:** `DinDin/spec-app-financeiro.md` + `DinDin/prototipo-dashboard.html` (pasta local fora do repo git, nome não relacionado à marca)
> **Criado em:** 2026-07-07
> **Decisão do usuário:** Frontend em **Angular** (a spec original sugeria React) e APIs em **Java**.
> Este arquivo é o documento-mestre. Cada sessão ganhará seu próprio SDD detalhado em
> `docs/session-NN-nome/SDD.md` no momento em que for iniciada (mesmo padrão do ContratoIA).

---

## 1. Stack definida

| Camada | Tecnologia | Observação |
|---|---|---|
| Backend | Java 21 + Spring Boot 3.5.x | Virtual threads; pacotes por feature (`transaction/`, `budget/`, `investment/`, `goal/`...) |
| API | REST + OpenAPI (springdoc) | Contrato documentado para futuro app mobile |
| Banco | PostgreSQL 16 (Docker Compose) | Dinheiro em `NUMERIC(14,2)` / `BigDecimal`; datas em `LocalDate` |
| Migrations | Flyway | Desde a V1 |
| Auth | Spring Security + JWT próprio | Mais leve que Keycloak para 1 usuário; multiusuário desde o início |
| Frontend | **Angular 20 + TypeScript** (standalone components, signals) | Adaptação da spec (React → Angular) |
| UI | Tailwind CSS + componentes próprios | Reproduzir o tema dark do protótipo (`prototipo-dashboard.html`) |
| Gráficos | Chart.js via ng2-charts | O protótipo já usa Chart.js (donut, barras, linha) |
| Import xlsx | Apache POI (backend) | Parser da Planilha_Gastos_2026 |
| Testes | JUnit 5 + Testcontainers (Postgres real); Karma/Jasmine no front | Meta de cobertura como no ContratoIA |
| CI | GitHub Actions (build + test + CodeQL/Trivy) | Mesmo padrão do ContratoIA |
| Deploy | Docker Compose em AWS Lightsail US$5 (ou EC2 t4g.micro) + Caddy | Conforme seção 6 da spec |

**Regras de ouro (da spec):** nunca float/double para dinheiro; API separada do front; saldo mensal é **cálculo**, não célula (`saldo(mês) = saldo(mês-1) + entradas − gastos`).

## 2. Estrutura do monorepo

```
Poupito/                  (repo git)
├── api/                    Spring Boot (Java 21, Maven)
│   └── src/main/java/com/poupito/api/
│       ├── auth/  account/  category/  transaction/  invoice/
│       ├── recurring/  budget/  goal/  investment/  dashboard/  importer/
│       └── common/         (config, security, erros, money)
├── web/                    Angular 20
│   └── src/app/
│       ├── core/           (auth, interceptors, api client, layout shell)
│       ├── features/       (dashboard, transactions, investments, goals, recurring, settings)
│       └── shared/         (cards, tabelas, month-picker, barras de progresso)
├── infra/                  docker-compose.yml (postgres, api, web/caddy), scripts
└── docs/                   PLANO-SDD.md (este arquivo) + session-NN-*/SDD.md
```

## 3. Modelo de dados

O schema completo está na seção 2 da spec (`spec-app-financeiro.md`) e vira as migrations Flyway:

- **V1:** `users`
- **V2:** `accounts` (CHECKING | CREDIT_CARD | CASH, closing_day/due_day), `categories` (EXPENSE | INCOME, icon, color)
- **V3:** `transactions` (EXPENSE | INCOME | INVOICE_ADJUSTMENT, account obrigatória, invoice_id), `card_invoices` (OPEN | CLOSED | PAID, declared_total)
- **V4:** `refresh_tokens` (sessão #S, segurança — não estava no plano original, entrou na frente por pedido do usuário)
- **V5:** `recurring_transactions` (fixos com day_of_month, active) — sessão #8
- **V6:** `budgets` (categoria × mês × valor esperado) — sessão #10
- **V7:** `goals` + `goal_contributions`
- **V8:** `investments` (RESERVA | RENDA_FIXA | RENDA_VARIAVEL) + `investment_entries` (APORTE | RESGATE | ATUALIZACAO_SALDO)

Cada migration entra na sessão que implementa a feature correspondente. Numeração vai sendo ajustada conforme a ordem real de merge (não a ordem do roadmap), já que sessões podem ficar com PR aberto aguardando aprovação do usuário antes de outra ser iniciada.

## 4. Regras de negócio críticas

1. **Vínculo de fatura:** lançamento em cartão de crédito é atribuído à `card_invoice` do período conforme `date` × `closing_day` do cartão.
2. **Fechamento de fatura:** usuário informa `declared_total`; se difere do somatório dos lançamentos, o app cria `INVOICE_ADJUSTMENT` com a diferença. Ao detalhar gastos esquecidos depois, o ajuste é reduzido automaticamente.
3. **Materialização de fixos:** job `@Scheduled` mensal cria transactions a partir de `recurring_transactions`, com flag "pago?".
4. **Rentabilidade (fase 2):** TWR simplificado por período: `(saldo_atual − saldo_anterior − aportes) / saldo_anterior`. Comparação com CDI via API SGS do Bacen (série 12, sem chave).
5. **Metas:** aporte mensal necessário = `(target_amount − acumulado) / meses_restantes`.

## 5. Sessões planejadas

### Fase 0 — Fundação

**#1 — Estrutura Inicial + Backend Base** ✅ CONCLUÍDA (2026-07-07 — SDD: `docs/session-01-estrutura-inicial/SDD.md`)
Tasks: (1) monorepo `api/`+`web/`+`infra/`+`docs/`; (2) Docker Compose com Postgres 16; (3) Spring Boot 3.5 + Flyway V1 (`users`) + springdoc + healthcheck; (4) verificação (`docker compose up`, `/actuator/health`, Swagger UI).
Pré-req: nenhum.

**#2 — Auth JWT** ✅ CONCLUÍDA (2026-07-08 — SDD: `docs/session-02-auth-jwt/SDD.md`; cobertura 97,3%, JaCoCo 90% enforçado desde aqui)
Tasks: (1) register/login com password hash (BCrypt) e emissão de JWT; (2) Spring Security filter chain + contexto do usuário; (3) testes (Testcontainers); (4) verificação end-to-end via curl.
Pré-req: #1.

**#3 — Setup Frontend Angular** ✅ CONCLUÍDA (2026-07-09 — SDD: `docs/session-03-setup-frontend/SDD.md`; cobertura 100%/90%/100%/100%)
Tasks: (1) Angular 20 + Tailwind + tema dark do protótipo (variáveis CSS `--bg`, `--card`, `--accent`...); (2) layout shell — sidebar (Dashboard, Transações, Investimentos, Metas, Fixos, Configurações), topbar com month-picker; (3) telas login/registro + interceptor JWT + guards; (4) verificação (login funcional contra a API).
Pré-req: #2.

**#4 — CI/CD** ✅ CONCLUÍDA (2026-07-09 — SDD: `docs/session-04-cicd/SDD.md`; 5 workflows verdes no PR #4; pendência manual: branch protection em main/develop). **Fase 0 completa.**
Tasks: (1) GitHub Actions backend (build, test); (2) frontend (build, test); (3) CodeQL + Trivy + Dependency Review; (4) verificação (pipelines verdes).
Pré-req: #1–#3 (pode rodar em paralelo com #5+).

### Fase 1 — MVP (substitui a planilha)

**#5 — Contas & Categorias** ✅ CONCLUÍDA (2026-07-09 — SDD: `docs/session-05-contas-categorias/SDD.md`)
Tasks: (1) migration V2 + CRUD `accounts` (tipos, closing/due day); (2) CRUD `categories` (icon, cor, kind); (3) tela Configurações no Angular; (4) verificação.
Pré-req: #3.

**#6 — Transações (backend)** ✅ CONCLUÍDA (2026-07-09 — SDD: `docs/session-06-transacoes-backend/SDD.md`)
Tasks: (1) migration V3 + CRUD com validações (conta obrigatória, BigDecimal); (2) regra de vínculo à fatura pelo closing_day; (3) filtros por mês/conta/categoria + paginação; (4) testes + verificação.
Pré-req: #5.

**#7 — Transações (frontend)** ✅ CONCLUÍDA (2026-07-09 — SDD: `docs/session-07-transacoes-frontend/SDD.md`)
Tasks: (1) tabela mensal com tags coloridas de categoria (como no protótipo); (2) modal de lançamento rápido (data = hoje, última conta usada); (3) edição/exclusão + filtros; (4) verificação.
Pré-req: #6.

**#S — Segurança (STRIDE + LGPD)** ✅ CONCLUÍDA (2026-07-11 — inserida a pedido do usuário; SDD: `docs/session-S-seguranca/SDD.md`; docs: `docs/security/`). Migration **V4** (`refresh_tokens`). Auth migrada para cookies httpOnly + refresh rotacionado/revogável; rate limiting; fail-fast de segredos em prod; security headers; endpoints LGPD de exportação/exclusão. 101 testes API + 77 web.

**#8 — Fixos Recorrentes** ✅ CONCLUÍDA (2026-07-12 — SDD: `docs/session-08-fixos-recorrentes/SDD.md`)
Tasks: (1) migration **V5** (V4 usada na sessão #S) + CRUD; (2) job mensal de materialização + flag "pago?"; (3) tela Fixos com checkbox; (4) testes do job + verificação.
Pré-req: #6.

**#9 — Fechamento de Fatura** ✅ CONCLUÍDA (2026-07-13 — SDD: `docs/session-09-fechamento-fatura/SDD.md`; sem migration)
Tasks: (1) ciclo de vida da fatura (OPEN → CLOSED → PAID) + total lançado vs. declarado; (2) lançamento automático de ajuste (INVOICE_ADJUSTMENT) e redução ao detalhar; (3) UI de fatura por cartão; (4) testes + verificação.
Pré-req: #6.

**#10 — Orçamentos (orçado vs. realizado)** ✅ CONCLUÍDA (2026-07-13 — SDD: `docs/session-10-orcamentos/SDD.md`)
Tasks: (1) migration **V6** (V5 reservada pela sessão #8) + CRUD budgets; (2) endpoint orçado × realizado por categoria/mês; (3) tabela com barras de progresso (vermelho ao estourar, como no protótipo); (4) verificação.
Pré-req: #6. 126 testes API (JaCoCo ≥90%), testes web com cobertura ≥90/80/90/90.

**#11 — Dashboard Mensal + Panorama Anual** ✅ CONCLUÍDA (2026-07-13 — SDD: `docs/session-11-dashboard/SDD.md`)
Tasks: (1) endpoints agregados (entradas, gastos, saldo do mês, saldo acumulado, gasto por categoria, série anual); (2) cards + donut de categorias + tabela orçado/realizado; (3) gráfico de barras anual (entradas × gastos); (4) verificação visual contra o protótipo. Chart.js puro (sem `ng2-charts`, que exigiria `@angular/cdk`). 168 testes API (JaCoCo ≥90%), 126 testes web (cobertura ≥90/80/90/90).
Pré-req: #7, #10.

**#12 — Import da Planilha xlsx** ✅ CONCLUÍDA (2026-07-14 — SDD: `docs/session-12-import-planilha/SDD.md`). **Fase 1 (MVP) completa.**
Tasks: (1) parser Apache POI da Planilha_Gastos_2026 (abas mensais, fixos, entradas); (2) endpoint de upload + mapeamento categorias/contas + idempotência; (3) UI de importação com preview; (4) verificação com a planilha real. Verificado com o arquivo real do usuário: 440 linhas, 439 transações criadas, idempotência confirmada (reimport pula tudo como duplicata). 183 testes API (JaCoCo ≥90%), 134 testes web (cobertura ≥90/80/90/90).
Pré-req: #11. **Critério de sucesso da Fase 1: abandonar a planilha no mês seguinte.**

### Fase 2 — Investimentos

**#13 — Investimentos (backend)** ✅ CONCLUÍDA (2026-07-14 — SDD: `docs/session-13-investimentos-backend/SDD.md`)
Tasks: (1) migration V7 + CRUD investments/entries (aporte, resgate, atualização de saldo); (2) cálculo de rentabilidade TWR por período e por classe; (3) testes + verificação. 207 testes API (JaCoCo ≥90%).
Pré-req: #2 (independente do MVP).

**#14 — Integração CDI (Bacen SGS)** ✅ CONCLUÍDA (2026-07-15 — SDD: `docs/session-14-integracao-cdi/SDD.md`)
Tasks: (1) client HTTP da série 12 com cache local; (2) endpoint carteira × CDI acumulado; (3) testes com mock + verificação. 216 testes API (JaCoCo ≥90%); verificado com a API real do Bacen.
Pré-req: #13.

**#15 — Investimentos (frontend)** ✅ CONCLUÍDA (2026-07-16 — SDD: `docs/session-15-investimentos-frontend/SDD.md`)
Tasks: (1) cards por classe (patrimônio total, reserva, RF, RV); (2) gráfico de linha patrimônio × CDI; (3) lançamentos de aporte/resgate/atualização; (4) verificação contra o protótipo. 171 testes web (cobertura ≥90/80/90/90).
Pré-req: #14.

**#16 — Metas Financeiras** ✅ CONCLUÍDA (2026-07-16 — SDD: `docs/session-16-metas-financeiras/SDD.md`). **Fase 2 (Investimentos) completa.**
Tasks: (1) migration **V9** (não V6, já usada pela sessão #10) + CRUD goals/contributions + cálculo de aporte necessário; (2) tela Metas com barras de progresso e "R$ X/mês até data"; (3) verificação. 239 testes API (JaCoCo ≥90%), 152 testes web (cobertura ≥90/80/90/90).
Pré-req: #13.

### Fase 3 — Qualidade de vida

**#17 — Alertas de Orçamento + Busca e Tags** ✅ CONCLUÍDA (2026-07-16 — SDD: `docs/session-17-alertas-busca-tags/SDD.md`). **Primeira sessão da Fase 3.**
Tasks: (1) alerta ao estourar orçamento (badge/notificação no app); (2) busca full-text e filtros avançados; (3) tags livres em transações; (4) verificação. Migration **V10** (`transaction_tags`). 197 testes web (cobertura ≥90/80/90/90); verificado end-to-end no browser.
Pré-req: #11.

**#18 — Parcelamentos** ✅ CONCLUÍDA (2026-07-17 — SDD: `docs/session-18-parcelamentos/SDD.md`)
Tasks: (1) compra em N× gera N transações futuras vinculadas; (2) UI de parcelamento no lançamento; (3) verificação. Migration **V11**. 252 testes API + 206 testes web (cobertura ≥90/80/90/90); verificado end-to-end no browser.
Pré-req: #9.

**#19 — Export CSV/xlsx** ✅ CONCLUÍDA (2026-07-17 — SDD: `docs/session-19-export-csv-xlsx/SDD.md`)
Tasks: (1) endpoint de export (POI); (2) botão de export com filtros aplicados; (3) verificação. 259 testes API + 209 testes web (cobertura ≥90/80/90/90); verificado com arquivos reais (CSV e xlsx).
Pré-req: #7.

**#20 — PWA** ✅ CONCLUÍDA (2026-07-17 — SDD: `docs/session-20-pwa/SDD.md`)
Tasks: (1) `@angular/pwa` (manifest + service worker); (2) ajustes responsive mobile (sidebar vira drawer off-canvas com botão hambúrguer abaixo de 700px); (3) verificação. 213 testes web (cobertura ≥90/80/90/90); verificado end-to-end com service worker registrado, manifest servido e drawer mobile funcionando (login real + viewport mobile no browser).
Pré-req: #11.

**#21 — Deploy AWS** ✅ CÓDIGO CONCLUÍDO (2026-07-18 — SDD: `docs/session-21-deploy-aws/SDD.md`) — **provisionamento real pendente do usuário**
Tasks: (1) `web/Dockerfile` + `Caddyfile` (Caddy serve o estático do Angular e faz proxy `/api/*`; `api/Dockerfile` já existia da sessão #4) + `infra/docker-compose.prod.yml`; (2) **Lightsail US$5/mês** (decisão do usuário — logo, imagens `linux/amd64`, não ARM); (3) `infra/scripts/backup.sh` (pg_dump → S3, lifecycle 30 dias) + `setup-host.sh` (swap 2GB); (4) `.github/workflows/deploy.yml` (SSH manual via `workflow_dispatch`) + job `docker` novo em `ci-web.yml`; (5) smoke test local do compose completo (proxy, fallback SPA, healthcheck) — verificação end-to-end **em produção real** ainda pendente: requer o usuário comprar domínio, criar a instância Lightsail e configurar os secrets do GitHub (passo a passo em `infra/README.md`).
Pré-req: #4 + MVP estável (recomendado após #12).

### Fase 4 — Open Finance

**#22 — Conexão bancária via Open Finance** (sessão formal, a refinar quando a Fase 3 terminar)
Ideia inicial: conectar contas de banco via agregador certificado em Open Finance (Pluggy ou Belvo) para puxar extratos/saldos automaticamente, reduzindo lançamento manual.
Tasks a refinar: (1) avaliar Pluggy vs. Belvo (custo por conta conectada, cobertura de bancos, free tier); (2) fluxo de consentimento OAuth do usuário com o banco (conexão, expiração/reautenticação de token); (3) endpoint/job de sincronização periódica de extratos → mapeamento para `transactions` (evitar duplicidade com lançamentos manuais); (4) UI de gerenciamento de conexões bancárias; (5) verificação end-to-end com conta de banco real (sandbox do agregador).
Pré-req: Fase 3 completa (MVP estável + deploy). Risco/trade-off a decidir: custo recorrente por conta conectada escala com base de usuários — mais vantajoso enquanto uso é pessoal (poucas contas) do que se o produto virar SaaS multiusuário sem repasse desse custo.

**#23 — Identidade visual (logo + marca + paleta oficial)** ✅ CONCLUÍDA (SDDs: rename `docs/session-23-rebrand-guaranin/SDD.md` (2026-07-19), rename `docs/session-23-rebrand-poupito/SDD.md` (2026-07-22), visual `docs/session-23-identidade-visual/SDD.md` (2026-07-22))
**Nome:** "DinDin" → "Guaranin" (2026-07-19) → "**Poupito**" (2026-07-22, final — "Guaranin" soava a guaraná/guarani; "Poupito", de "poupar", comunica economizar). Domínio `poupito.com` a registrar pelo usuário.
**Identidade visual ("Crescimento Seguro"):** logo "P" azul-marinho + broto verde (fornecido pelo usuário); paleta navy `#0F172A`/`#1E293B` + verde esmeralda `#059669`/`#10B981` + neutras branco/cinza-claro; slogan "Descomplique, poupe, Poupito.". Implementado: **tema claro como padrão + toggle dark/light** (`ThemeService`, variáveis CSS `:root`/`[data-theme="dark"]`, script inline anti-flash); sidebar navy como "chrome" da marca nos dois temas; ícones PWA + favicon reais gerados do logo (substituem o placeholder do Angular da sessão #20); gráficos Chart.js e tints semânticos passam a respeitar o tema. 217 testes web; verificado nos dois temas no browser.
**Melhorias futuras (não bloqueiam):** self-hostar fonte Inter; recolorir gráficos ao alternar tema sem navegar; favicon multi-resolução.
Pré-req: nenhuma.

**#24 — Observabilidade + Hardening contra exaustão** (sessão formal, a refinar quando a Fase 3 terminar — recomendado rodar **antes** de #22 apesar do número mais alto, mesma lógica da sessão #S)
Ideia inicial: hoje só `actuator/health`/`info` estão expostos e só login/registro tem rate limiting (`LoginRateLimiter`, por IP+email) — o resto da API (transações, export, import) não tem limite nenhum, e a instância Lightsail de 1GB é um alvo fácil de exaustão assim que ficar pública. Motivador direto: o job de sincronização periódica do Open Finance (#22) é exatamente o tipo de coisa que falha silenciosamente sem observabilidade — melhor ter isso pronto antes.
Tasks a refinar: (1) logs estruturados (JSON) por nível, com foco em eventos de segurança (falha de login, hits no rate limiter, 401/429, 5xx); (2) exportar esses logs pra **CloudWatch** (fora da instância — sobrevive a um comprometimento onde o atacante apaga logs locais) com retenção curta (30-90 dias) pra controlar custo, mais um **CloudWatch Alarm** simples (ex.: pico de tentativas de login falhas) notificando por email; (3) rate limiting geral na API (não só auth) — provavelmente via módulo de rate limit do Caddy ou filtro genérico no Spring, com limite mais agressivo nos endpoints caros (export xlsx via Apache POI, import de planilha); (4) **Cloudflare gratuito** na frente da instância — proteção DDoS básica + rate limiting na borda antes mesmo de chegar no Lightsail, mais barato/efetivo que construir tudo na aplicação; (5) revisar limites de connection pool (HikariCP) e threads (Tomcat) pra não estourar a RAM da instância sob carga; (6) verificação: simular rajada de requisições e confirmar que a aplicação se protege sem cair.
Pré-req: #21 (precisa da instância real rodando em produção pra fazer sentido testar isso de verdade).

**#25 — Remodelagem Contas & Cartões (método de pagamento)** ✅ CONCLUÍDA (2026-07-22 — SDD: `docs/session-25-remodelagem-contas-cartoes/SDD.md`) — **prioridade sobre #24/#22 (decisão do usuário: o rearranjo de domínio vem antes do resto)**
Motivação: cartão de crédito como tipo de conta mistura "onde o dinheiro vive" com "instrumento de pagamento"; e o importador pulava a regra de fatura (bug: aba Faturas zerada após import) — corrigido dentro desta sessão.
Decisões (usuário): entidade **Card** separada sempre vinculada a uma conta; compra no crédito só debita a conta **quando a fatura é paga** (`INVOICE_PAYMENT`, excluído das agregações de gasto pra não contar duas vezes); **dados transacionais recomeçados** na migration V12 (app pré-produção). Método de pagamento (crédito/débito/dinheiro) é **derivado**, não digitado; parcelamento passa a exigir cartão; fixos seguem em conta (cartão em fixos = melhoria futura).
Tasks: (1) migration V12 + CRUD `/v1/cards`; (2) transações xor conta/cartão + fatura por cartão + endpoint pagar fatura; (3) importer roteando cartão→fatura; (4) testes backend; (5) frontend (painel Cartões, "Pagar com", badge de método, pagar fatura, import); (6) testes web + verificação e2e; (7) docs + PR.
Pré-req: nenhum técnico (roda já).

### Fase 5 — Futuro (sem sessão planejada ainda)

Multi-tenancy real, plano free/pago, cotações via brapi.dev, app mobile consumindo a mesma API, feature "a receber/emprestado" (contas mãe).

## 6. Grafo de dependências (resumo)

```
#1 → #2 → #3 → #4
      #2 ─────────→ #13 → #14 → #15
      #3 → #5 → #6 → #7 ──┐        #13 → #16
                 #6 → #8   ├→ #11 → #12
                 #6 → #9   │   #11 → #17, #20
                 #6 → #10 ─┘   #9 → #18   #7 → #19
                               #4 + #12 → #21
```

Sessões #13–#16 (investimentos) podem rodar em paralelo com a Fase 1 a partir da sessão #2.

## 7. Decisões em aberto (herdadas da spec)

- Categoria "Itaú" da planilha mistura conta com categoria → no import (#12), mapear cartão como conta e pedir categoria real.
- "Contas mãe" (empréstimos a familiares) → avaliar feature "a receber" na Fase 3/4.
- Keycloak em vez de JWT próprio se SSO for necessário no futuro (migração possível sem quebrar a API).
