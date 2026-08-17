import { formatBRL, type GastoCategoria, type Transacao } from './tipos';

/* ============================================================
   Gráficos em SVG puro — sem biblioteca.
   Motivo: um donut e uma linha são ~40 linhas de matemática.
   Chart.js/Recharts pesam 150-400KB e só valem quando você
   precisa de zoom, tooltip complexo, eixos configuráveis etc.
   ============================================================ */

/* ---------------- DONUT: proporção dos gastos ---------------- */
export function Donut({ dados, total }: { dados: GastoCategoria[]; total: number }) {
  const R = 60;          // raio
  const ESPESSURA = 22;
  const circunferencia = 2 * Math.PI * R;

  let acumulado = 0;

  if (total <= 0) {
    return <p className="vazio">Sem gastos pra exibir.</p>;
  }

  return (
    <div className="donut-wrap">
      <svg viewBox="0 0 160 160" className="donut">
        {/* trilho de fundo */}
        <circle cx="80" cy="80" r={R} fill="none" stroke="var(--surface-2)" strokeWidth={ESPESSURA} />

        {dados.map((g) => {
          const fracao = g.totalCents / total;
          const comprimento = fracao * circunferencia;
          // dasharray desenha o pedaço; dashoffset gira ele pro lugar certo
          const el = (
            <circle
              key={g.categoriaId ?? 'sem'}
              cx="80"
              cy="80"
              r={R}
              fill="none"
              stroke={g.cor ?? 'var(--text-faint)'}
              strokeWidth={ESPESSURA}
              strokeDasharray={`${comprimento} ${circunferencia - comprimento}`}
              strokeDashoffset={-acumulado}
              transform="rotate(-90 80 80)"
            >
              <title>{`${g.nome ?? 'Sem categoria'}: ${formatBRL(g.totalCents)}`}</title>
            </circle>
          );
          acumulado += comprimento;
          return el;
        })}

        {/* valor no centro */}
        <text x="80" y="76" className="donut-rotulo" textAnchor="middle">total</text>
        <text x="80" y="94" className="donut-valor" textAnchor="middle">
          {(total / 100).toLocaleString('pt-BR', {
            style: 'currency', currency: 'BRL',
            notation: total >= 1000000 ? 'compact' : 'standard',
            maximumFractionDigits: total >= 1000000 ? 1 : 0,
          })}
        </text>
      </svg>

      <ul className="legenda">
        {dados.map((g) => (
          <li key={g.categoriaId ?? 'sem'}>
            <i className="ponto" style={{ background: g.cor ?? 'var(--text-faint)' }} />
            <span className="leg-nome">{g.nome ?? 'Sem categoria'}</span>
            <span className="leg-pct">{((g.totalCents / total) * 100).toFixed(0)}%</span>
            <span className="leg-valor">{formatBRL(g.totalCents)}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

/* ------------- LINHA: gasto acumulado no mês ------------- */
export function LinhaEvolucao({
  transacoes,
  ano,
  mes,
}: {
  transacoes: Transacao[];
  ano: number;
  mes: number;
}) {
  const diasNoMes = new Date(ano, mes, 0).getDate();
  const hoje = new Date();
  const mesAtual = hoje.getFullYear() === ano && hoje.getMonth() + 1 === mes;
  const ultimoDia = mesAtual ? hoje.getDate() : diasNoMes;

  // gasto por dia (transferência não conta)
  const porDia = new Array(diasNoMes + 1).fill(0);
  transacoes
    .filter((t) => t.tipo === 'SAIDA')
    .forEach((t) => {
      const dia = Number(t.data.slice(8, 10));
      porDia[dia] += -t.valorCents;
    });

  // acumula
  const acumulado: number[] = [];
  let soma = 0;
  for (let d = 1; d <= ultimoDia; d++) {
    soma += porDia[d];
    acumulado.push(soma);
  }

  const total = soma;
  if (total <= 0 || acumulado.length < 2) {
    return <p className="vazio">Sem gastos pra exibir neste mês.</p>;
  }

  // area de desenho (unidades do viewBox; o CSS estica pra largura real)
  const L = 720, A = 300;
  const ESQ = 62, DIR = 16, TOPO = 18, BASE = 34;

  const maxV = Math.max(...acumulado);
  // "teto redondo": arredonda o topo pra cima, pro eixo ter numero limpo
  const passo = Math.pow(10, Math.floor(Math.log10(maxV))) / 2;
  const teto = Math.ceil(maxV / passo) * passo;

  const x = (i: number) => ESQ + (i / (acumulado.length - 1)) * (L - ESQ - DIR);
  const y = (v: number) => A - BASE - (v / teto) * (A - TOPO - BASE);

  const pontos = acumulado.map((v, i) => `${x(i)},${y(v)}`).join(' ');
  const area = `${ESQ},${A - BASE} ${pontos} ${x(acumulado.length - 1)},${A - BASE}`;

  // 4 linhas de grade horizontais
  const grade = [0, 0.25, 0.5, 0.75, 1].map((f) => ({ f, v: teto * f }));

  // marca a cada ~5 dias no eixo x
  const passoDia = Math.max(1, Math.ceil(acumulado.length / 6));
  const diasEixo = acumulado
    .map((_, i) => i)
    .filter((i) => i % passoDia === 0 || i === acumulado.length - 1);

  const mediaDiaria = total / acumulado.length;
  const projecao = mediaDiaria * diasNoMes;

  return (
    <div className="linha-wrap">
      <div className="linha-topo">
        <div>
          <span className="linha-rotulo">Gasto acumulado</span>
          <strong className="linha-total">{formatBRL(total)}</strong>
        </div>
        {mesAtual && (
          <div className="linha-projecao">
            <span className="linha-rotulo">Projeção do mês</span>
            <strong>{formatBRL(projecao)}</strong>
          </div>
        )}
      </div>

      <svg viewBox={`0 0 ${L} ${A}`} className="gr-linha-grande">
        <defs>
          <linearGradient id="fadeOuro" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="var(--gold)" stopOpacity="0.32" />
            <stop offset="100%" stopColor="var(--gold)" stopOpacity="0" />
          </linearGradient>
        </defs>

        {/* grade + valores do eixo Y */}
        {grade.map(({ f, v }) => (
          <g key={f}>
            <line
              x1={ESQ} x2={L - DIR}
              y1={y(v)} y2={y(v)}
              stroke="var(--border)" strokeWidth="1"
              strokeDasharray={f === 0 ? undefined : '3 4'}
            />
            <text x={ESQ - 10} y={y(v) + 4} className="eixo" textAnchor="end">
              {v === 0 ? '0' : (v / 100).toLocaleString('pt-BR', {
                notation: 'compact', maximumFractionDigits: 0,
              })}
            </text>
          </g>
        ))}

        {/* dias no eixo X */}
        {diasEixo.map((i) => (
          <text key={i} x={x(i)} y={A - 12} className="eixo" textAnchor="middle">
            {i + 1}
          </text>
        ))}

        <polygon points={area} fill="url(#fadeOuro)" />
        <polyline
          points={pontos}
          fill="none"
          stroke="var(--gold)"
          strokeWidth="2.5"
          strokeLinejoin="round"
          strokeLinecap="round"
        />
        <circle cx={x(acumulado.length - 1)} cy={y(total)} r="5" fill="var(--gold)" />
        <circle cx={x(acumulado.length - 1)} cy={y(total)} r="9" fill="var(--gold)" opacity="0.25" />
      </svg>
    </div>
  );
}

/* ------------- BARRAS: comparativo entradas x saídas ------------- */
export function BarrasComparativo({ entradas, saidas }: { entradas: number; saidas: number }) {
  const maior = Math.max(entradas, saidas, 1);
  const item = (rotulo: string, valor: number, cor: string) => (
    <div className="comp-item">
      <div className="comp-topo">
        <span>{rotulo}</span>
        <strong>{formatBRL(valor)}</strong>
      </div>
      <div className="barra-trilho">
        <div className="barra-preenche" style={{ width: `${(valor / maior) * 100}%`, background: cor }} />
      </div>
    </div>
  );

  return (
    <div className="comparativo">
      {item('Entradas', entradas, 'var(--positive)')}
      {item('Saídas', saidas, 'var(--negative)')}
    </div>
  );
}
