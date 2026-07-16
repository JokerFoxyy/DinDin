export type AssetClass = 'RESERVA' | 'RENDA_FIXA' | 'RENDA_VARIAVEL';
export type EntryType = 'APORTE' | 'RESGATE' | 'ATUALIZACAO_SALDO';

export interface Investment {
  id: string;
  name: string;
  assetClass: AssetClass;
  institution: string;
}

export interface InvestmentEntry {
  id: string;
  date: string;
  type: EntryType;
  amount: number;
  balanceAfter: number | null;
}

export interface InvestmentPerformance {
  id: string;
  name: string;
  assetClass: AssetClass;
  institution: string;
  currentBalance: number;
  lastPeriodReturnPercentage: number | null;
}

export interface AssetClassPerformance {
  assetClass: AssetClass;
  totalBalance: number;
  lastPeriodReturnPercentage: number | null;
}

export interface InvestmentReport {
  investments: InvestmentPerformance[];
  byClass: AssetClassPerformance[];
}

export interface CdiPoint {
  date: string;
  accumulatedPercentage: number;
}

export interface InvestmentRequest {
  name: string;
  assetClass: AssetClass;
  institution: string;
}

export interface InvestmentUpdateRequest {
  name: string;
  institution: string;
}

export interface InvestmentEntryRequest {
  date: string;
  type: EntryType;
  amount: number;
  balanceAfter: number | null;
}
