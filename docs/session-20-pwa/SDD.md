# SDD — Sessão #20: PWA

> **Data:** 2026-07-17 · **Pré-req:** #11 · **Branch:** `feature/pwa` · **Plano-mestre:** `docs/PLANO-SDD.md`
> **Fase 3 — Qualidade de vida.**

## Objetivo

Deixar o DinDin instalável como app (ícone na tela inicial, funciona com cache offline básico) e usável de verdade no celular — a sidebar fixa de 220px não cabe numa tela pequena.

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| PWA | `ng add @angular/pwa` (padrão do Angular) | gera `manifest.webmanifest`, `ngsw-config.json` e registra o service worker (`provideServiceWorker`, `enabled: !isDevMode()`) sem reinventar a roda; já é o caminho oficial suportado pelo builder do projeto. |
| Manifest | Nome/short_name "DinDin", `theme_color`/`background_color` = `#0d1117` (mesmo `--bg` do tema escuro do app), ícones padrão do schematic (8 tamanhos, `maskable any`) | mantém consistência visual com o app; **os ícones são o placeholder gerado pelo Angular** (logo do framework) — não há asset de logo próprio do DinDin ainda, ficou registrado como pendência, não inventamos uma marca. |
| Service worker | Configuração padrão do schematic (`ngsw-config.json`: prefetch de `index.html`/JS/CSS, lazy+prefetch de assets estáticos) | suficiente para cache do app shell e funcionamento offline básico; não há necessidade de estratégia de cache customizada nesta sessão (sem dados para sincronizar offline). |
| Sidebar mobile | Vira **drawer off-canvas**: escondida por `transform: translateX(-100%)` abaixo de 700px, botão hambúrguer fixo abre com backdrop semi-transparente; fecha ao clicar num item de navegação ou no backdrop | breakpoint anterior (900px) já reorganizava a sidebar em barra horizontal rolável, mas isso ainda ocupava espaço vertical valioso em telas de celular real (~375–430px) — um drawer é o padrão esperado em apps mobile. Breakpoint intermediário (701–900px, tablets) manter o comportamento antigo de barra horizontal. |
| Estado da sidebar | `signal<boolean>` no `Shell` (`sidebarOpen`), sem persistir em localStorage | é estado de UI efêmero, não faz sentido lembrar entre sessões — sempre começa fechada. |

## Tasks

- **TASK-1** — `ng add @angular/pwa`; customizar `manifest.webmanifest` (nome, cores) e `index.html` (title, theme-color meta).
- **TASK-2** — `Shell`: `sidebarOpen` signal + `toggleSidebar()`/`closeSidebar()`; botão hambúrguer, backdrop, drawer com transição; fechar ao navegar ou clicar fora.
- **TASK-3** — Testes web (Karma) para toggle/close do menu mobile; build de produção + verificação do service worker e manifest servidos; verificação end-to-end no browser (login real, redimensionamento para viewport mobile, abrir/fechar drawer, navegação fecha o menu).

## Status

- [x] TASK-1 — `@angular/pwa`
- [x] TASK-2 — Sidebar colapsável mobile
- [x] TASK-3 — Testes + verificação

**Resultado da verificação (2026-07-17):**
- Web: `npm run test:ci` — **213 testes**, cobertura 96,66/84,86/93,16/96,59% (acima de 90/80/90/90).
- `npm run build:prod`: `dist/web/browser/` contém `ngsw-worker.js`, `ngsw.json` e `manifest.webmanifest` gerados corretamente.
- **Verificação end-to-end real**: servindo o build com `ng serve --configuration production` (para habilitar o service worker, que fica desabilitado em modo dev) + API/Postgres reais rodando via Docker — login real, `navigator.serviceWorker.getRegistrations()` confirmou `ngsw-worker.js` registrado, `fetch('/manifest.webmanifest')` retornou nome "DinDin", tema `#0d1117` e 8 ícones. Redimensionando a viewport para 375×812 (mobile): o botão hambúrguer aparece, clicar nele adiciona a classe `open` na sidebar e o backdrop some/aparece corretamente; clicar num item de navegação fecha o menu e navega (`/transacoes`); clicar no backdrop fecha sem navegar. Confirmado via inspeção do DOM real (classes, `getComputedStyle`, CSSOM das regras `@media`) e pelos disparos de clique reais no navegador.
- **Limitação desta verificação**: a captura de screenshot pixel-a-pixel do Browser pane apresentou um travamento pontual nesta sessão (renderer não repintava mesmo após forçar `!important` inline, e o `getBoundingClientRect` ficava desatualizado); a verificação funcional foi feita via inspeção de DOM/CSSOM e disparo real de eventos de clique, que confirmaram o comportamento correto, mas não há screenshot visual anexado.
- **Pendência conhecida**: ícones do manifest são o placeholder padrão do `@angular/pwa` (logo do Angular) — falta um ícone próprio do DinDin quando houver um asset de marca definido.
