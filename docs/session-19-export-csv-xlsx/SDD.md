# SDD — Sessão #19: Export CSV/xlsx

> **Data:** 2026-07-17 · **Pré-req:** #7 · **Branch:** `feature/export-csv-xlsx` · **Plano-mestre:** `docs/PLANO-SDD.md`
> **Fase 3 — Qualidade de vida.**

## Objetivo

Caminho de volta para o Excel: exportar as transações do mês (com os mesmos filtros já aplicados na tela) em CSV ou xlsx.

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Endpoint | `GET /v1/transactions/export?month=&accountId=&categoryId=&type=&q=&tag=&format=csv\|xlsx` (default `csv`) | reaproveita exatamente os mesmos parâmetros de filtro do `GET /v1/transactions` (sessão #6/#17) — o que o usuário vê filtrado na tela é o que sai no arquivo. |
| Paginação | nenhuma — exporta **todas** as transações que casam com o filtro no mês, não só a página atual | é um export, não uma visualização paginada; volume mensal de uso pessoal é pequeno. |
| Geração | `TransactionExportService` novo, reaproveita `TransactionSpecifications.search(...)` (sessão #17) sem paginar; CSV escrito manualmente (RFC 4180 — aspas/escaping só quando necessário); xlsx via Apache POI (já é dependência do projeto, sessão #12 — import) | evita adicionar biblioteca de CSV só pra isso; POI já suporta o schema necessário (cabeçalho + linhas + formatação numérica). |
| Colunas | Data, Descrição, Conta, Categoria, Tipo, Valor, Tags, Parcela, Fatura | espelha as colunas já visíveis na tela de Transações (sessões #6/#17/#18) — nada de novo pro usuário decodificar. |
| Formato numérico (xlsx) | célula `NUMERIC` com format `#,##0.00` (nunca texto formatado como "R$ 31,73") | segue a diretriz de planilha financeira: fórmulas/valores nativos, não texto — permite o usuário somar/filtrar no Excel depois. |
| Nome do arquivo | `transacoes-{mês}.csv` / `.xlsx` (ex. `transacoes-2026-07.csv`) | previsível, fácil de identificar depois de baixado várias vezes. |

## Tasks

- **TASK-1 — Backend**: `TransactionExportService` (busca sem paginação + monta linhas), `TransactionController` ganha `GET /v1/transactions/export`.
- **TASK-2 — Testes backend**: geração CSV (conteúdo e escaping), geração xlsx (linhas e formatação via Apache POI), filtros aplicados corretamente, `Content-Disposition`/`Content-Type`; JaCoCo ≥90%.
- **TASK-3 — Frontend**: botão "Exportar" (dropdown CSV/xlsx) na tela de Transações, usando os filtros/mês atuais da tela; download via blob.
- **TASK-4 — Testes web + verificação**: specs Angular (Karma ≥90/80/90/90) + verificação end-to-end no browser (download real, abrir o arquivo gerado).

## Status

- [x] TASK-1 — Backend
- [x] TASK-2 — Testes backend
- [x] TASK-3 — Frontend
- [x] TASK-4 — Testes web + verificação

**Resultado da verificação (2026-07-17):**
- API: `mvnw verify` — **259 testes** (geração CSV com escaping, xlsx com Apache POI, filtros aplicados, headers `Content-Disposition`/`Content-Type`), JaCoCo ≥90%, BUILD SUCCESS.
- Web: `npm run test:ci` — **209 testes**, cobertura 96,64/84,86/93,1/96,58% (acima de 90/80/90/90).
- **Verificação end-to-end real**: `curl` direto contra a API local gerou um CSV real (`transacoes-2026-07.csv`, escaping de aspas/vírgula confirmado byte a byte) e um xlsx real (reconhecido como "Microsoft Excel 2007+" válido); pela UI, os botões "Exportar CSV"/"Exportar xlsx" na tela de Transações dispararam as chamadas corretas (`GET /v1/transactions/export?month=...&format=csv|xlsx`, ambas 200 OK).
