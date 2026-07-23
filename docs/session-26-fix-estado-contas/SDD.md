# SDD — Sessão #26: Bugfix estado de contas/cartões dessincronizado na UI

**Branch:** `feature/fix-estado-contas-dessincronizado` (a partir da #25, que ainda não está em develop)
**Data:** 2026-07-22
**Tipo:** Bugfix (frontend only — backend já está correto)

## Contexto / bugs reportados (usuário, 2026-07-22)

1. Criar cartão vinculado a uma conta retornava **"Erro ao salvar o cartão"**.
2. Selects de conta **listavam contas já apagadas** (ex.: "Débito").

## Causa-raiz

Origem única para os dois sintomas: cada componente carrega listas de referência (contas, cartões, categorias) **independentemente em `ngOnInit`** e nunca ressincroniza. Ao apagar uma conta no `accounts-panel` (que se recarrega sozinho), os dropdowns dos outros componentes continuam com a lista antiga em cache — mostram contas fantasma (sintoma 2). Ao escolher uma conta fantasma e salvar o cartão, o backend responde **404 "Conta não encontrada"** (`CardService.ownedAccount`), exibido como o genérico "Erro ao salvar o cartão" (sintoma 1). Confirmado no banco: a conta escolhida não existia mais — o delete funcionou; a UI é que ficou dessincronizada.

## Solução

Introduzir **stores reativos baseados em signals** para as três coleções de referência compartilhadas entre telas — `AccountStore`, `CardStore`, `CategoryStore` (em `core/state/`). Cada store:
- mantém um `signal<T[]>` exposto como readonly, single source of truth do app;
- `ensureLoaded()` — carrega uma vez (idempotente) no `ngOnInit` de qualquer consumidor;
- `refresh()` — recarrega do backend;
- `create`/`update`/`delete` — delegam ao service existente e dão `refresh()` no sucesso (via `tap`), então **toda mutação propaga pra todos os consumidores** reativamente.

Consumidores passam a **ler o signal do store** (em vez de um signal local carregado no `ngOnInit`) e a mutar via store. Assim, apagar uma conta no painel de Contas atualiza na hora os dropdowns de Cartões, Transações, Faturas, Import e Fixos.

## Tasks

1. **`AccountStore`** + refatorar consumidores (`accounts-panel` muta; `cards-panel`, `transactions`, `invoices`, `importer`, `recurring` leem).
2. **`CardStore`** + refatorar consumidores (`cards-panel` muta; `transactions`, `importer` leem).
3. **`CategoryStore`** + refatorar consumidores (`categories-panel` muta; `transactions`, `importer`, `budgets` leem).
4. **Mensagem de erro específica no 404** ao salvar cartão/transação: "A conta/cartão selecionado não existe mais — atualize a lista" + refresh do store, em vez do genérico.
5. **UX "débito não é cartão"**: texto de orientação no painel de Cartões deixando claro que Cartões = só crédito; débito/dinheiro = contas (escolhidos no "Pagar com").
6. **Testes web** (≥90/80/90/90) atualizados + verificação e2e: apagar conta e confirmar que some de todos os selects sem recarregar a página.

## Fora de escopo

- Backend (correto). Delete bloqueado por FK (409) já tem mensagem; melhorar o texto entra na task 4/5 se barato.
- Reatividade de outras coleções não compartilhadas entre telas.

## Verificação

Karma ≥90/80/90/90 + e2e no browser: criar 2 contas → abrir form de cartão (vê as 2) → apagar 1 conta no painel de Contas → confirmar que o dropdown do form de cartão e o "Pagar com" de Transações **não** mostram mais a conta apagada, sem reload.
