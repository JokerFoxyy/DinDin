import {
  Component, ElementRef, Injector, OnDestroy, OnInit, ViewChild, afterNextRender, inject, signal
} from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import Chart from 'chart.js/auto';

import { InvestmentService } from './investment.service';
import {
  AssetClass, AssetClassPerformance, EntryType, Investment, InvestmentEntry, InvestmentReport
} from './investment.models';
import { AlignedSeries, alignSeries, buildBalanceTimeline, sumTimelines, weightedAverageReturn } from './investments.utils';

const CLASS_LABELS: Record<AssetClass, string> = {
  RESERVA: 'Reserva de emergência',
  RENDA_FIXA: 'Renda fixa',
  RENDA_VARIAVEL: 'Renda variável'
};

const ENTRY_TYPE_LABELS: Record<EntryType, string> = {
  APORTE: 'Aporte',
  RESGATE: 'Resgate',
  ATUALIZACAO_SALDO: 'Atualização'
};

const ASSET_CLASSES: AssetClass[] = ['RESERVA', 'RENDA_FIXA', 'RENDA_VARIAVEL'];

interface RecentEntryRow {
  entry: InvestmentEntry;
  investmentId: string;
  investmentName: string;
}

@Component({
  selector: 'app-investments',
  imports: [ReactiveFormsModule, CurrencyPipe, DatePipe],
  templateUrl: './investments.html',
  styleUrl: './investments.css'
})
export class Investments implements OnInit, OnDestroy {
  private readonly investmentService = inject(InvestmentService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly injector = inject(Injector);

  readonly classLabels = CLASS_LABELS;
  readonly entryTypeLabels = ENTRY_TYPE_LABELS;
  readonly assetClasses = ASSET_CLASSES;

  readonly investments = signal<Investment[]>([]);
  readonly report = signal<InvestmentReport | null>(null);
  readonly recentEntries = signal<RecentEntryRow[]>([]);
  readonly hasChartData = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly investmentModalOpen = signal(false);
  readonly editingInvestment = signal<Investment | null>(null);
  readonly entryModalOpen = signal(false);

  @ViewChild('chartCanvas') chartCanvasRef?: ElementRef<HTMLCanvasElement>;
  private chart?: Chart;

  readonly investmentForm = this.formBuilder.nonNullable.group({
    name: ['', Validators.required],
    assetClass: ['RENDA_FIXA' as AssetClass, Validators.required],
    institution: ['', Validators.required]
  });

  readonly entryForm = this.formBuilder.nonNullable.group({
    investmentId: ['', Validators.required],
    date: [today(), Validators.required],
    type: ['APORTE' as EntryType, Validators.required],
    amount: [null as number | null, [Validators.required, Validators.min(0)]],
    balanceAfter: [null as number | null]
  });

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  classPerformance(assetClass: AssetClass): AssetClassPerformance {
    const found = this.report()?.byClass.find((c) => c.assetClass === assetClass);
    return found ?? { assetClass, totalBalance: 0, lastPeriodReturnPercentage: null };
  }

  totalBalance(): number {
    return this.report()?.byClass.reduce((sum, c) => sum + c.totalBalance, 0) ?? 0;
  }

  totalReturnPercentage(): number | null {
    return weightedAverageReturn(this.report()?.byClass ?? []);
  }

  openCreateInvestment(): void {
    this.editingInvestment.set(null);
    this.errorMessage.set(null);
    this.investmentForm.reset({ name: '', assetClass: 'RENDA_FIXA', institution: '' });
    this.investmentModalOpen.set(true);
  }

  openEditInvestment(investment: Investment): void {
    this.editingInvestment.set(investment);
    this.errorMessage.set(null);
    this.investmentForm.reset({
      name: investment.name, assetClass: investment.assetClass, institution: investment.institution
    });
    this.investmentModalOpen.set(true);
  }

  closeInvestmentModal(): void {
    this.investmentModalOpen.set(false);
    this.editingInvestment.set(null);
  }

  submitInvestment(): void {
    if (this.investmentForm.invalid) {
      this.investmentForm.markAllAsTouched();
      return;
    }
    const raw = this.investmentForm.getRawValue();
    const editing = this.editingInvestment();
    const request$ = editing
      ? this.investmentService.update(editing.id, { name: raw.name, institution: raw.institution })
      : this.investmentService.create(raw);
    request$.subscribe({
      next: () => {
        this.closeInvestmentModal();
        this.load();
      },
      error: (error: HttpErrorResponse) =>
        this.errorMessage.set(error.error?.message ?? 'Erro ao salvar o investimento')
    });
  }

  removeInvestment(investment: Investment): void {
    this.investmentService.delete(investment.id).subscribe({
      next: () => this.load(),
      error: () => this.errorMessage.set('Erro ao excluir o investimento')
    });
  }

  openCreateEntry(): void {
    this.errorMessage.set(null);
    this.entryForm.reset({
      investmentId: this.investments()[0]?.id ?? '', date: today(), type: 'APORTE', amount: null, balanceAfter: null
    });
    this.entryModalOpen.set(true);
  }

  closeEntryModal(): void {
    this.entryModalOpen.set(false);
  }

  submitEntry(): void {
    if (this.entryForm.invalid) {
      this.entryForm.markAllAsTouched();
      return;
    }
    const raw = this.entryForm.getRawValue();
    if (raw.type === 'ATUALIZACAO_SALDO' && raw.balanceAfter === null) {
      this.errorMessage.set('Informe o saldo após a atualização');
      return;
    }
    this.investmentService.createEntry(raw.investmentId, {
      date: raw.date, type: raw.type, amount: raw.amount as number, balanceAfter: raw.balanceAfter
    }).subscribe({
      next: () => {
        this.closeEntryModal();
        this.load();
      },
      error: (error: HttpErrorResponse) =>
        this.errorMessage.set(error.error?.message ?? 'Erro ao registrar o lançamento')
    });
  }

  removeEntry(row: RecentEntryRow): void {
    this.investmentService.deleteEntry(row.investmentId, row.entry.id).subscribe({
      next: () => this.load(),
      error: () => this.errorMessage.set('Erro ao excluir o lançamento')
    });
  }

  private load(): void {
    forkJoin({
      investments: this.investmentService.list(),
      report: this.investmentService.report()
    }).subscribe(({ investments, report }) => {
      this.investments.set(investments);
      this.report.set(report);
      this.loadEntriesAndChart(investments);
    });
  }

  private loadEntriesAndChart(investments: Investment[]): void {
    if (investments.length === 0) {
      this.recentEntries.set([]);
      this.hasChartData.set(false);
      afterNextRender(() => this.destroyChart(), { injector: this.injector });
      return;
    }
    forkJoin(investments.map((investment) =>
      this.investmentService.listEntries(investment.id).pipe(catchError(() => of([] as InvestmentEntry[])))
    )).subscribe((entriesByInvestment) => {
      const rows: RecentEntryRow[] = investments
        .flatMap((investment, index) => entriesByInvestment[index]
          .map((entry) => ({ entry, investmentId: investment.id, investmentName: investment.name })))
        .sort((a, b) => b.entry.date.localeCompare(a.entry.date))
        .slice(0, 10);
      this.recentEntries.set(rows);

      const timelines = entriesByInvestment.map((entries) => buildBalanceTimeline(entries));
      const portfolio = sumTimelines(timelines);
      if (portfolio.length === 0) {
        this.hasChartData.set(false);
        afterNextRender(() => this.destroyChart(), { injector: this.injector });
        return;
      }
      this.investmentService.cdi(portfolio[0].date, today()).pipe(catchError(() => of([]))).subscribe((cdi) => {
        const aligned = alignSeries(portfolio, cdi);
        this.hasChartData.set(aligned.labels.length > 0);
        afterNextRender(() => this.renderChart(aligned), { injector: this.injector });
      });
    });
  }

  private destroyChart(): void {
    this.chart?.destroy();
    this.chart = undefined;
  }

  private renderChart(aligned: AlignedSeries): void {
    this.destroyChart();
    const canvas = this.chartCanvasRef?.nativeElement;
    if (!canvas || aligned.labels.length === 0) {
      return;
    }
    this.chart = new Chart(canvas, {
      type: 'line',
      data: {
        labels: aligned.labels,
        datasets: [
          {
            label: 'Patrimônio (R$)', data: aligned.portfolio, borderColor: '#4f8ef7',
            backgroundColor: 'transparent', yAxisID: 'y', tension: 0.2
          },
          {
            label: 'CDI acumulado (%)', data: aligned.cdi, borderColor: '#3fb950',
            backgroundColor: 'transparent', yAxisID: 'y1', tension: 0.2
          }
        ]
      },
      options: {
        animation: false,
        maintainAspectRatio: false,
        scales: {
          y: { type: 'linear', position: 'left', ticks: { color: '#8b949e' }, grid: { color: '#2a3240' } },
          y1: { type: 'linear', position: 'right', ticks: { color: '#8b949e' }, grid: { display: false } },
          x: { ticks: { color: '#8b949e' }, grid: { color: '#2a3240' } }
        },
        plugins: { legend: { labels: { color: '#8b949e' } } }
      }
    });
  }
}

function today(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}
