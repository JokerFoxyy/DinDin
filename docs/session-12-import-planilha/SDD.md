# SDD — Sessão #12: Import da Planilha xlsx

> **Data:** 2026-07-14 · **Pré-req:** #11 ✅ · **Branch:** `feature/import-planilha` · **Plano-mestre:** `docs/PLANO-SDD.md`
> **Critério de sucesso da Fase 1 (MVP):** abandonar a planilha no mês seguinte.

## Objetivo

Importar o histórico da `Planilha_Gastos_2026` (arquivo real do usuário, analisado nesta sessão) para popular `transactions` — sem duplicar dados, com revisão do usuário antes de confirmar (mapeamento de contas/categorias que não batem com o que já existe no app).

## Estrutura real da planilha (analisada com o arquivo do usuário)

18 abas: `Comece aqui`, `Categorias` (só explicativa), `Panorama anual`, `Metas Financeiras`, `Investimento`, 12 abas mensais (`Janeiro`...`Dezembro`), `Contas mãe março e abril` (tabela avulsa, fora de escopo).

Cada aba mensal segue um **template fixo** (linhas idênticas em todos os 12 meses, verificado programaticamente):

| Seção | Range | Colunas | Observação |
|---|---|---|---|
| **Fixos** | linha 9 (header) até ~21 | C=Nome, E=Pago?, F=Data, G=Tipo(conta), H=Categoria, I=Valor | gastos fixos do mês |
| **Cartão de Crédito** | linha 26 (header) em diante | C=Nome, D=Parcelas, F=Data, G=Tipo(conta), H=Categoria, I=Valor | `Parcelas` (ex. "20/20") ignorado nesta sessão — Valor já é a parcela do mês; parcelamento de verdade é a sessão futura #18 |
| **Gastos do Mês** | linha 9 (header) | K=Nome, L=Data, M=Tipo(conta), N=Categoria, O=Valor | contém linhas **"Diferença de totais"** — o próprio conceito que nosso `INVOICE_ADJUSTMENT` já automatiza; essas linhas são **puladas** no import |
| **Entradas** | linha 9 em diante | Q=Nome, R=Valor | só a(s) linha(s) antes de "Saldo mes anterior" são renda de verdade; o resto (Saldo, Total, Saídas, saldos de conta, Investimentos/Reserva/Renda fixa) é resumo/saldo calculado — **parada no primeiro rótulo de fronteira** (`saldo`, `total`, `saídas`, ou um nome de conta conhecido) |

**Fora de escopo desta sessão** (mapeiam para fases futuras): `Investimento`, `Metas Financeiras`, `Panorama anual` (dashboard já calcula isso), `Contas mãe` (Fase 3/4 — "a receber/emprestado").

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Parser | Apache POI, leitura por **posição fixa de linha/coluna** por seção (não busca dinâmica de header) | confirmado que o template é idêntico nas 12 abas — mais simples e mais robusto que heurística de busca |
| "Tipo" da planilha | mapeia para **conta** (`accounts.name`), não para `AccountType` | mesma observação já registrada em `CLAUDE.md`: "Categoria 'Itaú' mistura conta com categoria" |
| Categoria "Itaú" (cartão usado como categoria em algumas linhas) | fica pendente no fluxo de mapeamento (não bate com nenhuma categoria real) — usuário escolhe a categoria de verdade na revisão | resolve o problema apontado no `CLAUDE.md` sem lógica especial hard-coded |
| Linhas "Diferença de totais" | **puladas** (nunca viram transação) | o app já cria `INVOICE_ADJUSTMENT` automaticamente na sessão #9; importar essas linhas duplicaria o ajuste |
| Categoria das Entradas | a planilha não tem coluna de categoria para renda — usa a **descrição da linha** (ex. "Salário Final") como nome de categoria sugerido, revisável no mapeamento | evita inventar uma categoria fixa; usuário decide |
| Fluxo | **preview → mapeamento → commit**, em duas chamadas HTTP (reenvia o arquivo nas duas, sem estado no servidor) | simples, sem precisar de sessão/cache server-side pra um import único |
| Idempotência | antes de inserir, checa se já existe uma transação idêntica (usuário+data+valor+descrição+conta) | permite rodar o import de novo (ex. depois de corrigir um mapeamento) sem duplicar |
| Contas/categorias não mapeadas | preview retorna listas separadas de nomes de conta/categoria não encontrados; commit recebe o mapeamento (usar existente ou criar novo) | atende ao "endpoint de upload + mapeamento categorias/contas" do plano |
| Endpoints | `POST /v1/import/preview` (multipart: arquivo) → linhas + não-mapeados; `POST /v1/import/commit` (multipart: arquivo + mapeamento JSON) → cria contas/categorias faltantes, insere transações, resumo criado/pulado | REST, sem estado |
| Data da transação | **mês/ano da própria aba** (nome da aba = mês; ano vem de um parâmetro `year`, default 2026) + dia da coluna "Data" quando presente (senão dia 1) | a coluna "Data" na planilha real é inconsistente (linhas de Fixos costumam vir sem data; linhas de Cartão às vezes trazem a data da compra original de uma parcela, de anos anteriores) — usar mês/ano da aba evita contaminar o mês certo com datas de referência erradas |

## Tasks

- **TASK-1 — Parser**: `SpreadsheetParser` (Apache POI) lendo as 12 abas mensais nas posições fixas acima; `ImportRow` (sheet, seção, descrição, data, contaBruta, categoriaBruta, valor, type).
- **TASK-2 — ImportService**: `preview(bytes)` (linhas + não-mapeados, sem persistir) e `commit(bytes, mapping)` (cria contas/categorias pendentes, insere transações com checagem de duplicidade).
- **TASK-3 — ImportController** `/v1/import`.
- **TASK-4 — Testes backend**: parser com uma planilha de teste sintética (mesma estrutura, dados fictícios) cobrindo: Fixos, Cartão, Gastos do Mês (com "Diferença de totais" pulada), Entradas (parando no rótulo de fronteira); serviço (mapeamento de conta/categoria existente vs. criar novo, idempotência); JaCoCo ≥90%.
- **TASK-5 — Frontend**: tela de Importação (`/importar`) — upload → tabela de preview + formulário de mapeamento (contas/categorias não encontradas) → confirmar → resumo.
- **TASK-6 — Verificação end-to-end**: `mvnw verify` + `npm run test:ci` verdes; browser com planilha sintética de teste (mesma estrutura da real, dados fictícios — o arquivo real do usuário não entra no repo nem é usado no teste automatizado, só serviu de referência de estrutura nesta análise); PR com CI verde (entregue ao usuário).

## Status

- [x] TASK-1 — Parser
- [x] TASK-2 — ImportService
- [x] TASK-3 — ImportController
- [x] TASK-4 — Testes backend
- [x] TASK-5 — Frontend
- [x] TASK-6 — Verificação end-to-end

**Resultado da verificação (2026-07-14):**
- API: `mvnw verify` — **183 testes** (`SpreadsheetParserTest`, `ImportServiceTest`, `ImportFlowIntegrationTest` novos), JaCoCo ≥90%, BUILD SUCCESS
- Web: `npm run test:ci` — **134 testes**, cobertura 96,48/82,40/92,37/96,15% (acima de 90/80/90/90)
- **Verificação com a planilha real do usuário** (`Planilha_Gastos_2026.xlsx`, via chamadas HTTP diretas — upload pelo `<input type="file">` da UI não é automatizável neste ambiente de browser porque abre um diálogo nativo do SO; a UI em si foi verificada visualmente carregando corretamente com os campos de upload/ano/análise):
  - `preview`: **440 linhas** encontradas (132 Fixos + 257 Cartão + 39 Gastos do Mês + 12 Entradas); contas não mapeadas exatamente as esperadas (Nubank, Uniclass, Débito, Click, Uniclass platinum +, Black, e o placeholder de Entradas); categorias não mapeadas incluindo o caso conhecido "Itaú" (cartão usado como categoria em algumas linhas) e categorias ad hoc além da aba "Categorias" (ex.: nomes de pessoas em presentes/empréstimos)
  - `commit` (mapeamento default, tudo "criar novo"): **439 transações criadas, 1 duplicada** (duas linhas idênticas dentro da própria planilha — dedupe funcionou também intra-arquivo), **7 contas criadas, 18 categorias criadas**
  - `commit` de novo com o mesmo arquivo: **0 criadas, 440 puladas como duplicata** — idempotência confirmada
- Dois bugs reais encontrados e corrigidos durante os testes automatizados (não durante a verificação com o arquivo real, que já rodou depois dos fixes): (1) checagem de "Diferença de totais" comparava com a string errada (`"total"` em vez de `"totai"` — "totais" não contém "total" como substring, falta o "l"); (2) contadores `accountsCreated`/`categoriesCreated` incrementavam mesmo quando a conta/categoria já existia via mapeamento (só deveriam contar criação de fato)
