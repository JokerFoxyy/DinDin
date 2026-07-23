import {
  Component, ElementRef, Injector, OnDestroy, OnInit, ViewChild, afterNextRender, inject, signal
} from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import Chart from 'chart.js/auto';

import { MonthPicker } from '../../shared/month-picker';
import { DashboardService } from './dashboard.service';
import { AnnualPoint, DashboardSummary } from './dashboard.models';
import { chartTheme } from '../../core/theme/chart-theme';

@Component({
  selector: 'app-dashboard',
  imports: [MonthPicker, CurrencyPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit, OnDestroy {
  private readonly dashboardService = inject(DashboardService);
  private readonly injector = inject(Injector);

  readonly month = signal(currentMonth());
  readonly summary = signal<DashboardSummary | null>(null);
  readonly annual = signal<AnnualPoint[]>([]);

  @ViewChild('donutCanvas') donutCanvasRef?: ElementRef<HTMLCanvasElement>;
  @ViewChild('annualCanvas') annualCanvasRef?: ElementRef<HTMLCanvasElement>;

  private donutChart?: Chart;
  private annualChart?: Chart;

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.donutChart?.destroy();
    this.annualChart?.destroy();
  }

  onMonthChange(month: string): void {
    this.month.set(month);
    this.load();
  }

  barWidth(percentage: number): number {
    return Math.min(percentage, 100);
  }

  private load(): void {
    this.dashboardService.summary(this.month()).subscribe((summary) => {
      this.summary.set(summary);
      afterNextRender(() => this.renderDonut(summary), { injector: this.injector });
    });
    this.dashboardService.annual(this.month()).subscribe((annual) => {
      this.annual.set(annual);
      afterNextRender(() => this.renderAnnual(annual), { injector: this.injector });
    });
  }

  private renderDonut(summary: DashboardSummary): void {
    this.donutChart?.destroy();
    this.donutChart = undefined;
    const canvas = this.donutCanvasRef?.nativeElement;
    if (!canvas || summary.categorySpend.length === 0) {
      return;
    }
    const t = chartTheme();
    this.donutChart = new Chart(canvas, {
      type: 'doughnut',
      data: {
        labels: summary.categorySpend.map((c) => c.categoryName),
        datasets: [
          {
            data: summary.categorySpend.map((c) => c.total),
            backgroundColor: summary.categorySpend.map((c) => c.categoryColor ?? t.accent),
            borderWidth: 0
          }
        ]
      },
      options: {
        cutout: '65%',
        animation: false,
        maintainAspectRatio: false,
        plugins: { legend: { position: 'right', labels: { color: t.muted } } }
      }
    });
  }

  private renderAnnual(annual: AnnualPoint[]): void {
    this.annualChart?.destroy();
    this.annualChart = undefined;
    const canvas = this.annualCanvasRef?.nativeElement;
    if (!canvas || annual.length === 0) {
      return;
    }
    const t = chartTheme();
    this.annualChart = new Chart(canvas, {
      type: 'bar',
      data: {
        labels: annual.map((p) => monthLabel(p.month)),
        datasets: [
          { label: 'Entradas', data: annual.map((p) => p.income), backgroundColor: t.green, borderRadius: 4 },
          { label: 'Gastos', data: annual.map((p) => p.expense), backgroundColor: t.red, borderRadius: 4 }
        ]
      },
      options: {
        animation: false,
        maintainAspectRatio: false,
        scales: {
          x: { ticks: { color: t.muted }, grid: { color: t.grid } },
          y: { ticks: { color: t.muted }, grid: { color: t.grid } }
        },
        plugins: { legend: { labels: { color: t.muted } } }
      }
    });
  }
}

function currentMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

function monthLabel(month: string): string {
  const [year, monthValue] = month.split('-').map(Number);
  const name = new Intl.DateTimeFormat('pt-BR', { month: 'short' }).format(new Date(year, monthValue - 1, 1));
  return name.replace('.', '').replace(/^./, (c) => c.toUpperCase());
}
