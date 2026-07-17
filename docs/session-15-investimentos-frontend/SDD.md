# SDD — Sessão #15: Investimentos (frontend)

> **Data:** 2026-07-16 · **Pré-req:** #14 · **Branch:** `feature/investimentos-frontend` · **Plano-mestre:** `docs/PLANO-SDD.md`
> **Fase 2 — Investimentos.** Fecha a fase 2 junto com a #16 (Metas).

## Objetivo

Tela `/investimentos` consumindo os endpoints das sessões #13/#14: cards de patrimônio por classe, lista de investimentos com rentabilidade, CRUD de investimento e de lançamentos (aporte/resgate/atualização), e o gráfico "Evolução do patrimônio × CDI" do protótipo.

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Fonte do gráfico | calculado **no cliente** a partir de `GET /v1/investments/{id}/entries` de cada investimento (mesma máquina de estados do `InvestmentReturnCalculator` do backend, reimplementada em `investments.utils.ts`) | a sessão #13/#14 não expõem uma série histórica pronta do patrimônio total — só saldo atual + rentabilidade do último período (por investimento/classe) e o acumulado do CDI num intervalo. Recalcular no cliente evita criar um novo endpoint só para o gráfico. |
| Eixos do gráfico | dois eixos Y: patrimônio total em **R$** (linha) × CDI acumulado em **%** (linha, eixo secundário) | mistura grandeza absoluta com percentual — não dá pra normalizar sem inventar uma base arbitrária pro patrimônio; dois eixos é a solução padrão pra "valor × benchmark %". |
| Eixo X | `category` (rótulos de data em string), igual ao gráfico anual do Dashboard (sessão #11) | evitar depender de adapter de tempo do Chart.js (`date-fns`/`luxon`, não instalado); uso pessoal com poucos lançamentos não precisa de escala de tempo real. |
| Série unificada | união das datas de todos os lançamentos + datas do CDI, com **forward-fill** (repete o último valor conhecido) para cada série nos pontos que não têm dado novo | as duas séries não têm datas coincidentes (Bacen só publica dias úteis; lançamentos são esporádicos) — forward-fill é a forma padrão de alinhar séries de frequências diferentes num mesmo eixo categórico. |
| CDI: intervalo buscado | `from` = data do lançamento mais antigo entre todos os investimentos, `to` = hoje | acompanha o período coberto pelos dados reais do usuário. |
| Cards de classe | sempre as 3 classes fixas (`RESERVA`, `RENDA_FIXA`, `RENDA_VARIAVEL`), com saldo 0 e sem rentabilidade quando a classe não tem investimento ainda | replica o layout fixo de 4 cards do protótipo (`prototipo-dashboard.html`, página Investimentos) mesmo com poucos dados cadastrados. |
| CRUD | dois modais (padrão dos orçamentos, sessão #10): "Novo investimento" (nome/classe/instituição — classe trava na edição, é imutável na API) e "Novo lançamento" (escolhe o investimento, tipo, data, valor, saldo após — obrigatório só em atualização de saldo) | reaproveita o padrão de modal já validado em `budgets.ts`/`.html`. |
| Metas de patrimônio (painel do protótipo) | **fora de escopo** — é a sessão #16 | o protótipo mistura os dois na mesma página, mas o plano trata como sessões separadas. |

## Tasks

- **TASK-1 — Modelos e serviço**: `investment.models.ts`, `investment.service.ts` (CRUD investimentos/entries, `report()`, `cdi(from, to)`).
- **TASK-2 — Cards + lista**: cards de patrimônio total/por classe, tabela de investimentos com rentabilidade do último período.
- **TASK-3 — Gráfico patrimônio × CDI**: `investments.utils.ts` (máquina de estados + merge de séries, testável isoladamente) + Chart.js dual-axis.
- **TASK-4 — CRUD na UI**: modais de investimento e de lançamento; tabela de últimos lançamentos.
- **TASK-5 — Testes + verificação**: specs Angular (Karma ≥90/80/90/90) + verificação visual no browser contra o protótipo.

## Status

- [x] TASK-1 — Modelos e serviço
- [x] TASK-2 — Cards + lista
- [x] TASK-3 — Gráfico patrimônio × CDI
- [x] TASK-4 — CRUD na UI
- [x] TASK-5 — Testes + verificação

**Resultado da verificação (2026-07-16):**
- `npm run test:ci` — **171 testes**, cobertura 96,46/83,64/92,4/96,39% (acima de 90/80/90/90).
- `investments.utils.ts` isola a lógica de reconstrução do patrimônio (máquina de estados) e o alinhamento de séries (`alignSeries`), testados isoladamente sem depender do componente.
- **Verificação end-to-end real no browser** (API + Postgres via Docker Compose + `ng serve`): criado um investimento via UI, registrada uma `ATUALIZACAO_SALDO` inicial (R$ 1.000, 01/06), um `APORTE` de R$ 100 (15/06) e uma segunda `ATUALIZACAO_SALDO` (R$ 1.120, 30/06) — a UI calculou e exibiu corretamente **patrimônio R$ 1.120,00 com rentabilidade +2% no último período**, tanto no card total quanto no card da classe Renda fixa e na linha do investimento na tabela; `GET /v1/investments/cdi?from=2026-06-01&to=2026-07-15` retornou dados reais do Bacen sem erro. Confirmado via árvore de acessibilidade + texto da página + requisições de rede (todas 200/201) — capturas de tela não foram possíveis nesta sessão por um travamento do mecanismo de screenshot do painel de browser (infra, não relacionado à aplicação).
