# SDD — Sessão #14: Integração CDI (Bacen SGS)

> **Data:** 2026-07-15 · **Pré-req:** #13 · **Branch:** `feature/integracao-cdi` (a partir de `feature/investimentos-backend`, ainda não mergeada em `develop` — rebase quando o PR #29 mergear) · **Plano-mestre:** `docs/PLANO-SDD.md`
> **Fase 2 — Investimentos.** Backend apenas; o gráfico "carteira × CDI" (frontend) é a sessão #15.

## Objetivo

Buscar a série histórica do CDI na API pública do Banco Central (SGS, série 12 — taxa DI diária, sem necessidade de chave/autenticação), com cache local em banco, e expor um endpoint com o CDI acumulado (composto) num intervalo — para o frontend (#15) sobrepor à evolução do patrimônio calculada na sessão #13.

## API do Bacen (SGS série 12)

```
GET https://api.bcb.gov.br/dados/serie/bcdata.sgs.12/dados?formato=json&dataInicial=DD/MM/AAAA&dataFinal=DD/MM/AAAA
→ [{"data":"02/01/2026","valor":"0.038932"}, ...]
```

- `valor`: taxa DI **diária**, em percentual (ex.: `0.038932` = 0,038932% ao dia).
- Só retorna dias úteis (fins de semana/feriados não aparecem) — não dá pra assumir uma linha por dia corrido.
- Sem autenticação, sem rate limit documentado, mas é serviço externo de terceiro — precisa de tratamento de falha (timeout/erro vira **502**, não deve derrubar o resto da API).

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Migration | **V8** (`cdi_rates`: `date` PK, `daily_rate` `NUMERIC(10,6)`) | cache local — dado histórico do Bacen nunca muda, então persistir é seguro e barato. |
| Cliente HTTP | `RestClient` (Spring 6.1+, já disponível no Boot 3.5) | nativo do Spring, sem dependência nova; testável com `MockRestServiceServer.bindTo(RestClient.Builder)`. |
| Estratégia de cache | Se já existe `cdi_rates` com `date = to` (data final pedida, truncada para no máximo ontem — Bacen não tem dado do dia corrente), assume o intervalo `[from, to]` completo em cache (a API só é chamada em blocos contínuos, nunca parcial) e não rechama o Bacen; senão, busca o intervalo inteiro do Bacen e grava os dias retornados (`ON CONFLICT (date) DO NOTHING`, idempotente) | cache "tudo ou nada" por bloco é simples de raciocinar e suficiente pro uso pessoal (poucas chamadas, sempre nos mesmos intervalos de relatório); evita heurística cara de detectar buracos individuais. |
| Cálculo do acumulado | composto: `acumulado(t) = Π(1 + taxa_i/100) − 1` para `i` de `from` até `t`, exposto como série (um ponto por dia com dado no Bacen) já com o percentual acumulado até aquele dia | permite ao frontend plotar a curva do CDI ponto a ponto ao lado da curva do patrimônio (mesmo eixo de tempo), não só o total do período. |
| Erro do Bacen (timeout/5xx/parse) | `ExternalServiceException` → **502 Bad Gateway** (novo handler em `GlobalExceptionHandler`) | não é erro do usuário nem 500 genérico — sinaliza claramente que a falha é de um serviço externo. |
| Endpoint | `GET /v1/investments/cdi?from=YYYY-MM-DD&to=YYYY-MM-DD` → lista de `{date, accumulatedPercentage}` | par do endpoint de patrimônio da sessão #13; parâmetros obrigatórios (sem default de período — quem decide a janela é o frontend, na #15, com base nas datas reais de `investment_entries`). |

## Tasks

- **TASK-1 — Migration V8 + entidade `CdiRate`**: tabela `cdi_rates`, repositório.
- **TASK-2 — `BacenCdiClient`**: `RestClient` configurado, parse do JSON do Bacen, `ExternalServiceException` em falha.
- **TASK-3 — `CdiService`**: cache-aside (verifica `cdi_rates`, busca no Bacen se necessário, grava, calcula acumulado composto) + `CdiController`.
- **TASK-4 — Testes**: `BacenCdiClient` com `MockRestServiceServer`; `CdiService` com mocks de repositório; integração via `@SpringBootTest` (Bacen real "mockado" no nível do `RestClient` — sem chamada de rede real no CI); JaCoCo ≥90%.

## Status

- [x] TASK-1 — Migration V8 + entidade `CdiRate`
- [x] TASK-2 — `BacenCdiClient`
- [x] TASK-3 — `CdiService` + `CdiController`
- [x] TASK-4 — Testes + verificação

**Resultado da verificação (2026-07-15):**
- `mvnw verify` — **216 testes** (`BacenCdiClientTest` com `MockRestServiceServer`, `CdiServiceTest`, `CdiFlowIntegrationTest` com `@MockitoBean` no cliente Bacen), JaCoCo ≥90%, BUILD SUCCESS.
- **Verificação com a API real do Banco Central** (`GET /v1/investments/cdi?from=2026-06-01&to=2026-06-10` contra a API local rodando com Postgres real via Docker Compose): retornou a série real do CDI acumulado de junho/2026, pulando corretamente os fins de semana (04 e 07/06); segunda chamada idêntica respondeu em ~180ms (cache local servido sem round-trip ao Bacen).
