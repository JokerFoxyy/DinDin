# SDD — Sessão #23 (parte 3): Identidade Visual "Crescimento Seguro"

> **Data:** 2026-07-22 · **Branch:** `feature/rebrand-poupito` (empilhado sobre o rename) · **Plano-mestre:** `docs/PLANO-SDD.md`
> **Fase 4.** Fecha a parte "identidade visual" da sessão #23 (as partes 1/2 foram os renames DinDin→Guaranin→Poupito).

## Objetivo

Aplicar a identidade visual oficial do Poupito (conceito "Crescimento Seguro"), definida pelo usuário: logo com o "P" azul-marinho + broto verde, paleta navy/esmeralda, tipografia sans moderna, interface limpa com cantos arredondados. Slogan: **"Descomplique, poupe, Poupito."** Público: pessoas em geral querendo organizar a vida financeira sem jargão; tom amigável e encorajador.

## Diretrizes recebidas (do usuário)

| Papel | Cor | Uso |
|---|---|---|
| Principal / segurança | Azul-marinho `#0F172A` / `#1E293B` | fundo (no dark) e "chrome" de navegação (sidebar) — confiança/estabilidade |
| Ação / destaque | Verde esmeralda `#059669` / `#10B981` | botões, CTA, saldo positivo, links — crescimento |
| Neutras | Branco `#FFFFFF` / cinza-claro `#F3F4F6` | cards e fundo no tema claro — interface limpa/legível |
| Tipografia | Sans moderna (Inter/Poppins/Roboto) | — |
| Estilo | Limpo, minimalista, cantos levemente arredondados | — |

## Decisões técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Direção do tema | **Claro como padrão + toggle dark/light** (escolha do usuário) | as diretrizes pedem cards brancos/interface clara; o dark continua disponível pra quem prefere. |
| Mecânica do tema | Tudo via variáveis CSS em `styles.css`: `:root` (light) + `:root[data-theme="dark"]` (override). `ThemeService` (signal + localStorage `poupito.theme`, respeita `prefers-color-scheme` na 1ª vez). Script inline no `index.html` aplica o `data-theme` **antes** do Angular subir (evita flash). | o app já roteava todas as cores por variáveis, então a troca foi centralizada; o script inline evita "flash" de cor errada. |
| Sidebar | Azul-marinho **igual nos dois temas** (variáveis `--brand-navy*`/`--sidebar-*` que não mudam) | é o "chrome"/elemento de segurança da marca; fica consistente independente do tema do conteúdo. |
| Tipografia | Stack `'Inter', 'Segoe UI', Roboto, system-ui, sans-serif` (sem bundle de fonte própria) | honra "sans moderna" com zero dependência nova e sem risco de bundle/offline; self-hostar Inter fica como melhoria futura. |
| Gráficos (Chart.js) | Helper `core/theme/chart-theme.ts` lê as variáveis CSS no momento de montar o gráfico | tira as cores fixas do tema escuro antigo; os gráficos respeitam o tema ativo ao carregar/navegar. Limitação: alternar o tema com um gráfico já na tela só o recolore após navegar/recarregar. |
| Ícones PWA | Símbolo "P" recortado da `Imagem_Sem_Fundo.png` (detecção da banda vertical do símbolo, separando do wordmark), gerados via Pillow: 8 tamanhos transparentes (`purpose: any`) + 2 maskable com fundo branco e 22% de safe-zone; `apple-touch-icon` 180; `favicon.ico` fornecido pelo usuário | os ícones antigos eram o placeholder do Angular (pendência da sessão #20); agora usam a marca real. |
| Cores semânticas | Tints de status/tag via `color-mix(in srgb, var(--X) 15%, transparent)` | ficam consistentes com a paleta e corretos nos dois temas (antes eram rgba fixos do tema escuro). |

## Paleta final (referência)

**Light (`:root`):** bg `#f3f4f6` · card `#ffffff` · card2 `#f8fafc` · border `#e2e8f0` · text `#0f172a` · muted `#64748b` · accent `#059669` · red `#dc2626` · yellow `#d97706` · purple `#7c3aed`.
**Dark (`[data-theme="dark"]`):** bg `#0f172a` · card `#1e293b` · card2 `#263449` · border `#334155` · text `#e2e8f0` · muted `#94a3b8` · accent `#10b981` · red `#f87171` · yellow `#fbbf24` · purple `#a78bfa`.
**Marca (fixo):** brand-navy `#0f172a` · brand-navy-2 `#1e293b` · logo verde `#34d399` · sidebar-text `#e2e8f0` · sidebar-muted `#94a3b8`.

## Verificação (2026-07-22)

- Web: `npm run test:ci` — **217 testes** (novos: `ThemeService` + toggle no shell), cobertura 96,73/84,81/93,27/96,66% (≥90/80/90/90); `build:prod` ok.
- **End-to-end no browser** (API + Postgres reais), via inspeção de estilos computados:
  - **Tema claro** (padrão): body `#f3f4f6`, cards brancos, texto navy, sidebar navy (`#0f172a`), botão CTA verde esmeralda (`#059669`), logo "Poup"(esmeralda)+"ito"(navy). Botão do rodapé oferece "🌙 Tema escuro".
  - **Toggle → dark**: body/sidebar navy, cards `#1e293b`, escolha persistida em `localStorage`, texto do botão vira "☀️ Tema claro" após o change detection.
  - Telas conferidas em ambos: login, dashboard, transações (tabela/painel legíveis).
  - Título da aba e ícone: "Poupito", manifest com `theme_color #0F172A` / `background_color #F3F4F6`.

## Pendências / melhorias futuras

- Self-hostar a fonte Inter (offline garantido) em vez de depender do fallback de sistema.
- Recolorir gráficos Chart.js dinamicamente ao alternar o tema sem precisar navegar (assinar mudanças do `ThemeService`).
- Lockup completo (logo + wordmark) numa tela "sobre"/rodapé, e favicon multi-resolução gerado a partir do símbolo.
