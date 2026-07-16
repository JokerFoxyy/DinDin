import { AssetClassPerformance, InvestmentEntry } from './investment.models';
import { alignSeries, buildBalanceTimeline, sumTimelines, weightedAverageReturn } from './investments.utils';

function entry(date: string, type: InvestmentEntry['type'], amount: number, balanceAfter: number | null = null):
    InvestmentEntry {
  return { id: date + type, date, type, amount, balanceAfter };
}

describe('investments.utils', () => {
  describe('buildBalanceTimeline', () => {
    it('should accumulate aportes and resgates in chronological order', () => {
      const timeline = buildBalanceTimeline([
        entry('2026-01-05', 'APORTE', 1000),
        entry('2026-01-01', 'APORTE', 500)
      ]);

      expect(timeline).toEqual([
        { date: '2026-01-01', value: 500 },
        { date: '2026-01-05', value: 1500 }
      ]);
    });

    it('should replace the running balance on ATUALIZACAO_SALDO', () => {
      const timeline = buildBalanceTimeline([
        entry('2026-01-01', 'APORTE', 1000),
        entry('2026-01-31', 'ATUALIZACAO_SALDO', 0, 1120)
      ]);

      expect(timeline[1].value).toBe(1120);
    });

    it('should subtract resgates', () => {
      const timeline = buildBalanceTimeline([
        entry('2026-01-01', 'APORTE', 1000),
        entry('2026-01-10', 'RESGATE', 200)
      ]);

      expect(timeline[1].value).toBe(800);
    });

    it('should return empty timeline for no entries', () => {
      expect(buildBalanceTimeline([])).toEqual([]);
    });
  });

  describe('sumTimelines', () => {
    it('should forward-fill each timeline and sum by date', () => {
      const a = [{ date: '2026-01-01', value: 100 }, { date: '2026-01-10', value: 150 }];
      const b = [{ date: '2026-01-05', value: 50 }];

      const total = sumTimelines([a, b]);

      expect(total).toEqual([
        { date: '2026-01-01', value: 100 },
        { date: '2026-01-05', value: 150 },
        { date: '2026-01-10', value: 200 }
      ]);
    });

    it('should return empty array when there are no timelines', () => {
      expect(sumTimelines([])).toEqual([]);
    });
  });

  describe('alignSeries', () => {
    it('should align portfolio and cdi onto a shared date axis with forward-fill', () => {
      const portfolio = [{ date: '2026-01-01', value: 1000 }, { date: '2026-01-10', value: 1100 }];
      const cdi = [
        { date: '2026-01-02', accumulatedPercentage: 0.05 },
        { date: '2026-01-09', accumulatedPercentage: 0.4 }
      ];

      const aligned = alignSeries(portfolio, cdi);

      expect(aligned.labels).toEqual(['2026-01-01', '2026-01-02', '2026-01-09', '2026-01-10']);
      expect(aligned.portfolio).toEqual([1000, 1000, 1000, 1100]);
      expect(aligned.cdi).toEqual([0, 0.05, 0.4, 0.4]);
    });

    it('should return empty series when there is no data at all', () => {
      const aligned = alignSeries([], []);

      expect(aligned.labels).toEqual([]);
      expect(aligned.portfolio).toEqual([]);
      expect(aligned.cdi).toEqual([]);
    });
  });

  describe('weightedAverageReturn', () => {
    function performance(assetClass: AssetClassPerformance['assetClass'], totalBalance: number,
        lastPeriodReturnPercentage: number | null): AssetClassPerformance {
      return { assetClass, totalBalance, lastPeriodReturnPercentage };
    }

    it('should weight the return by each class balance', () => {
      const result = weightedAverageReturn([
        performance('RENDA_FIXA', 900, 2),
        performance('RENDA_VARIAVEL', 100, -6)
      ]);

      // (900*2 + 100*-6) / 1000 = 1.2
      expect(result).toBeCloseTo(1.2, 5);
    });

    it('should ignore classes without a computed return', () => {
      const result = weightedAverageReturn([
        performance('RENDA_FIXA', 900, 2),
        performance('RESERVA', 100, null)
      ]);

      expect(result).toBeCloseTo(2, 5);
    });

    it('should return null when there is no balance to weight', () => {
      expect(weightedAverageReturn([])).toBeNull();
      expect(weightedAverageReturn([performance('RESERVA', 0, null)])).toBeNull();
    });
  });
});
