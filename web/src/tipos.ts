// Tipos compartilhados entre os componentes + helpers de formatação.

export type Conta = { id: string; nome: string; tipo: string };
export type Saldo = { contaId: string; nome: string; saldoCents: number };
export type Categoria = { id: string; nome: string; tipo: string; cor: string | null };

export type Transacao = {
  id: string;
  accountId: string;
  categoryId: string | null;
  descricao: string | null;
  valorCents: number;
  data: string;
  tipo: string;
  parcelaNum: number | null;
  parcelaTotal: number | null;
  transferId: string | null;
  parcelamentoId: string | null;
  recurringRuleId: string | null;
};

export type Recorrencia = {
  id: string;
  accountId: string;
  categoryId: string | null;
  tipo: 'ENTRADA' | 'SAIDA';
  valorCents: number;
  descricao: string;
  frequencia: 'MENSAL' | 'SEMANAL' | 'ANUAL';
  dia: number;
  mes: number | null;
  dataInicio: string;
  dataFim: string | null;
  ultimaGeracao: string | null;
  ativa: boolean;
};

export const DIAS_SEMANA = ['Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado', 'Domingo'];

/** descreve a regra em português: "todo dia 5", "toda segunda", "todo 15 de março" */
export function descreverRecorrencia(r: Recorrencia): string {
  if (r.frequencia === 'SEMANAL') return `toda ${DIAS_SEMANA[r.dia - 1].toLowerCase()}`;
  if (r.frequencia === 'ANUAL') return `todo dia ${r.dia} de ${MESES[(r.mes ?? 1) - 1].toLowerCase()}`;
  return `todo dia ${r.dia}`;
}

export type GastoCategoria = {
  categoriaId: string | null;
  nome: string | null;
  cor: string | null;
  totalCents: number;
  quantidade: number;
};

export const TIPO_LABEL: Record<string, string> = {
  CORRENTE: 'Conta corrente',
  POUPANCA: 'Poupança',
  CARTEIRA: 'Carteira',
  CARTAO_CREDITO: 'Cartão de crédito',
};

export const TIPO_ICONE: Record<string, string> = {
  CORRENTE: '🏦',
  POUPANCA: '🐷',
  CARTEIRA: '👛',
  CARTAO_CREDITO: '💳',
};

export const CORES = [
  '#3b82f6', '#e5484d', '#30a46c', '#f5a524',
  '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16',
];

export const MESES = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro',
];

/** centavos (inteiro) -> "R$ 1.234,56" */
export function formatBRL(cents: number): string {
  return (cents / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

/** versão curta pra KPI: "R$ 1,2 mil" */
export function formatCompacto(cents: number): string {
  const v = Math.abs(cents) / 100;
  if (v >= 1000) {
    return (cents / 100).toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      notation: 'compact',
      maximumFractionDigits: 1,
    });
  }
  return formatBRL(cents);
}

/** "2026-08-15" -> "15/08" */
export function formatDataCurta(iso: string): string {
  const [, m, d] = iso.split('-');
  return `${d}/${m}`;
}

/** "2026-08-15" -> "15/08/2026" */
export function formatData(iso: string): string {
  return iso.split('-').reverse().join('/');
}
