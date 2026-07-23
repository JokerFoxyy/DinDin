/**
 * Lê as cores do tema ativo a partir das variáveis CSS (definidas em
 * styles.css e trocadas por [data-theme]). Os gráficos Chart.js usam isso
 * no momento em que são montados, então ficam corretos para o tema vigente
 * ao carregar/navegar. (Alternar o tema com um gráfico já na tela só o
 * recolore após navegar/recarregar — limitação aceita.)
 */
export interface ChartTheme {
  muted: string;
  grid: string;
  accent: string;
  green: string;
  red: string;
}

export function chartTheme(): ChartTheme {
  const css = getComputedStyle(document.documentElement);
  const read = (name: string, fallback: string) => css.getPropertyValue(name).trim() || fallback;
  return {
    muted: read('--muted', '#64748b'),
    grid: read('--border', '#e2e8f0'),
    accent: read('--accent', '#059669'),
    green: read('--green', '#059669'),
    red: read('--red', '#dc2626')
  };
}
