# DinDin — Plano SDD de Implementação

> **Fonte:** `DinDin/spec-app-financeiro.md` + `DinDin/prototipo-dashboard.html`
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
DinDin/                  (repo git)
├── api/                    Spring Boot (Java 21, Maven)
│   └── src/main/java/com/dindin/api/
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
- **V4:** `recurring_transactions` (fixos com day_of_month, active)
- **V5:** `budgets` (categoria × mês × valor esperado)
- **V6:** `goals` + `goal_contributions`
- **V7:** `investments` (RESERVA | RENDA_FIXA | RENDA_VARIAVEL) + `investment_entries` (APORTE | RESGATE | ATUALIZACAO_SALDO)

Cada migration entra na sessão que implementa a feature correspondente.

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

**#7 — Transações (frontend)**
Tasks: (1) tabela mensal com tags coloridas de categoria (como no protótipo); (2) modal de lançamento rápido (data = hoje, última conta usada); (3) edição/exclusão + filtros; (4) verificação.
Pré-req: #6.

**#8 — Fixos Recorrentes**
Tasks: (1) migration V4 + CRUD; (2) job mensal de materialização + flag "pago?"; (3) tela Fixos com checkbox; (4) testes do job + verificação.
Pré-req: #6.

**#9 — Fechamento de Fatura**
Tasks: (1) ciclo de vida da fatura (OPEN → CLOSED → PAID) + total lançado vs. declarado; (2) lançamento automático de ajuste (INVOICE_ADJUSTMENT) e redução ao detalhar; (3) UI de fatura por cartão; (4) testes + verificação.
Pré-req: #6.

**#10 — Orçamentos (orçado vs. realizado)**
Tasks: (1) migration V5 + CRUD budgets; (2) endpoint orçado × realizado por categoria/mês; (3) tabela com barras de progresso (vermelho ao estourar, como no protótipo); (4) verificação.
Pré-req: #6.

**#11 — Dashboard Mensal + Panorama Anual**
Tasks: (1) endpoints agregados (entradas, gastos, saldo do mês, saldo acumulado, gasto por categoria, série anual); (2) cards + donut de categorias + tabela orçado/realizado; (3) gráfico de barras anual (entradas × gastos); (4) verificação visual contra o protótipo.
Pré-req: #7, #10.

**#12 — Import da Planilha xlsx**
Tasks: (1) parser Apache POI da Planilha_Gastos_2026 (abas mensais, fixos, entradas); (2) endpoint de upload + mapeamento categorias/contas + idempotência; (3) UI de importação com preview; (4) verificação com a planilha real.
Pré-req: #11. **Critério de sucesso da Fase 1: abandonar a planilha no mês seguinte.**

### Fase 2 — Investimentos

**#13 — Investimentos (backend)**
Tasks: (1) migration V7 + CRUD investments/entries (aporte, resgate, atualização de saldo); (2) cálculo de rentabilidade TWR por período e por classe; (3) testes + verificação.
Pré-req: #2 (independente do MVP).

**#14 — Integração CDI (Bacen SGS)**
Tasks: (1) client HTTP da série 12 com cache local; (2) endpoint carteira × CDI acumulado; (3) testes com mock + verificação.
Pré-req: #13.

**#15 — Investimentos (frontend)**
Tasks: (1) cards por classe (patrimônio total, reserva, RF, RV); (2) gráfico de linha patrimônio × CDI; (3) lançamentos de aporte/resgate/atualização; (4) verificação contra o protótipo.
Pré-req: #14.

**#16 — Metas Financeiras**
Tasks: (1) migration V6 + CRUD goals/contributions + cálculo de aporte necessário; (2) tela Metas com barras de progresso e "R$ X/mês até data"; (3) verificação.
Pré-req: #13.

### Fase 3 — Qualidade de vida

**#17 — Alertas de Orçamento + Busca e Tags**
Tasks: (1) alerta ao estourar orçamento (badge/notificação no app); (2) busca full-text e filtros avançados; (3) tags livres em transações; (4) verificação.
Pré-req: #11.

**#18 — Parcelamentos**
Tasks: (1) compra em N× gera N transações futuras vinculadas; (2) UI de parcelamento no lançamento; (3) verificação.
Pré-req: #9.

**#19 — Export CSV/xlsx**
Tasks: (1) endpoint de export (POI); (2) botão de export com filtros aplicados; (3) verificação.
Pré-req: #7.

**#20 — PWA**
Tasks: (1) `@angular/pwa` (manifest + service worker); (2) ajustes responsive mobile (sidebar colapsável, já esboçado no protótipo); (3) verificação (instalável no celular).
Pré-req: #11.

**#21 — Deploy AWS**
Tasks: (1) Dockerfiles ARM64 (JVM tunada `-XX:MaxRAMPercentage=50`) + compose de produção com Caddy (TLS + proxy `/api`); (2) Lightsail US$5 (decisão da spec; alternativa EC2 t4g.micro + Savings Plan); (3) backup pg_dump → S3 com lifecycle 30 dias + swap 2 GB; (4) GitHub Actions deploy via SSH; (5) verificação end-to-end em produção.
Pré-req: #4 + MVP estável (recomendado após #12).

### Fase 4 — Futuro (sem sessão planejada ainda)

Multi-tenancy real, plano free/pago, Open Finance (Pluggy/Belvo), cotações via brapi.dev, app mobile consumindo a mesma API, feature "a receber/emprestado" (contas mãe).

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
