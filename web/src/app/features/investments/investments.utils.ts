import { AssetClassPerformance, CdiPoint, InvestmentEntry } from './investment.models';

export interface DatedValue {
  date: string;
  value: number;
}

/**
 * Reimplementa a máquina de estados do `InvestmentReturnCalculator` (API) no cliente:
 * APORTE soma, RESGATE subtrai, ATUALIZACAO_SALDO substitui o saldo corrente pelo
 * `balanceAfter` informado. Produz um ponto por lançamento, na ordem cronológica.
 */
export function buildBalanceTimeline(entries: InvestmentEntry[]): DatedValue[] {
  const sorted = [...entries].sort((a, b) => a.date.localeCompare(b.date));
  let balance = 0;
  const points: DatedValue[] = [];
  for (const entry of sorted) {
    if (entry.type === 'APORTE') {
      balance += entry.amount;
    } else if (entry.type === 'RESGATE') {
      balance -= entry.amount;
    } else {
      balance = entry.balanceAfter ?? balance;
    }
    points.push({ date: entry.date, value: balance });
  }
  return points;
}

/** Soma o saldo de várias linhas do tempo (uma por investimento) num total por data. */
export function sumTimelines(timelines: DatedValue[][]): DatedValue[] {
  const allDates = new Set<string>();
  for (const timeline of timelines) {
    for (const point of timeline) {
      allDates.add(point.date);
    }
  }
  const dates = [...allDates].sort();
  return dates.map((date) => ({
    date,
    value: timelines.reduce((total, timeline) => total + valueAtOrBefore(timeline, date), 0)
  }));
}

/** Último valor conhecido numa linha do tempo ordenada, na data ou antes dela (0 se ainda não começou). */
function valueAtOrBefore(timeline: DatedValue[], date: string): number {
  let value = 0;
  for (const point of timeline) {
    if (point.date > date) {
      break;
    }
    value = point.value;
  }
  return value;
}

export interface AlignedSeries {
  labels: string[];
  portfolio: number[];
  cdi: number[];
}

/** Alinha patrimônio e CDI num único eixo de datas, repetindo (forward-fill) o último valor conhecido de cada série. */
export function alignSeries(portfolio: DatedValue[], cdi: CdiPoint[]): AlignedSeries {
  const cdiTimeline: DatedValue[] = cdi.map((point) => ({ date: point.date, value: point.accumulatedPercentage }));
  const allDates = new Set<string>([...portfolio.map((p) => p.date), ...cdiTimeline.map((p) => p.date)]);
  const labels = [...allDates].sort();
  return {
    labels,
    portfolio: labels.map((date) => valueAtOrBefore(portfolio, date)),
    cdi: labels.map((date) => valueAtOrBefore(cdiTimeline, date))
  };
}

/** Média da rentabilidade do último período por classe, ponderada pelo saldo atual de cada classe. */
export function weightedAverageReturn(byClass: AssetClassPerformance[]): number | null {
  const withReturn = byClass.filter((c) => c.lastPeriodReturnPercentage !== null && c.totalBalance > 0);
  const totalBalance = withReturn.reduce((sum, c) => sum + c.totalBalance, 0);
  if (totalBalance === 0) {
    return null;
  }
  const weightedSum = withReturn.reduce(
    (sum, c) => sum + c.totalBalance * (c.lastPeriodReturnPercentage as number), 0);
  return weightedSum / totalBalance;
}
