import { BarrasComparativo, Donut, LinhaEvolucao } from './Graficos';
import {
  formatBRL,
  formatDataCurta,
  MESES,
  type Categoria,
  type GastoCategoria,
  type Saldo,
  type Transacao,
} from './tipos';

type Props = {
  saldos: Saldo[];
  transacoesMes: Transacao[];
  recentes: Transacao[];
  relatorio: GastoCategoria[];
  categorias: Categoria[];
  ano: number;
  mes: number;
  onTrocarMes: (ano: number, mes: number) => void;
  onVerConta: (contaId: string) => void;
};

export default function Dashboard({
  saldos,
  transacoesMes,
  recentes,
  relatorio,
  categorias,
  ano,
  mes,
  onTrocarMes,
  onVerConta,
}: Props) {
  const patrimonio = saldos.reduce((s, x) => s + x.saldoCents, 0);

  // KPIs do mês: transferência NÃO entra (dinheiro só trocou de bolso)
  const doMes = transacoesMes.filter((t) => t.tipo !== 'TRANSFERENCIA');
  const entradas = doMes.filter((t) => t.valorCents > 0).reduce((s, t) => s + t.valorCents, 0);
  const saidas = doMes.filter((t) => t.valorCents < 0).reduce((s, t) => s - t.valorCents, 0);
  const resultado = entradas - saidas;

  const totalGasto = relatorio.reduce((s, g) => s + g.totalCents, 0);
  const nomeConta = (id: string) => saldos.find((s) => s.contaId === id)?.nome ?? '';

  function mesAnterior() {
    const d = new Date(ano, mes - 2, 1);
    onTrocarMes(d.getFullYear(), d.getMonth() + 1);
  }
  function mesSeguinte() {
    const d = new Date(ano, mes, 1);
    onTrocarMes(d.getFullYear(), d.getMonth() + 1);
  }

  return (
    <>
      {/* ---- VEREDITO: a resposta pra "estou bem?" ---- */}
      <section className="veredito">
        <span className="veredito-rotulo">Patrimônio total</span>
        <strong className={patrimonio < 0 ? 'veredito-valor negativo' : 'veredito-valor'}>
          {formatBRL(patrimonio)}
        </strong>
        <span className="veredito-nota">
          {saldos.length} {saldos.length === 1 ? 'conta' : 'contas'}
        </span>
      </section>

      {/* ---- navegação de mês ---- */}
      <div className="mes-nav">
        <button className="btn-icone" onClick={mesAnterior} title="Mês anterior">‹</button>
        <span className="mes-atual">{MESES[mes - 1]} {ano}</span>
        <button className="btn-icone" onClick={mesSeguinte} title="Próximo mês">›</button>
      </div>

      {/* ---- KPIs do mês ---- */}
      <section className="kpis">
        <article className="kpi">
          <span className="kpi-rotulo">Entradas</span>
          <strong className="kpi-valor positivo">{formatBRL(entradas)}</strong>
        </article>
        <article className="kpi">
          <span className="kpi-rotulo">Saídas</span>
          <strong className="kpi-valor negativo">{formatBRL(saidas)}</strong>
        </article>
        <article className="kpi">
          <span className="kpi-rotulo">Resultado</span>
          <strong className={resultado < 0 ? 'kpi-valor negativo' : 'kpi-valor positivo'}>
            {resultado >= 0 ? '+' : ''}{formatBRL(resultado)}
          </strong>
        </article>
        <article className="kpi">
          <span className="kpi-rotulo">Lançamentos</span>
          <strong className="kpi-valor">{doMes.length}</strong>
        </article>
      </section>

      {/* ---- palco: linha grande à esquerda, apoio à direita ---- */}
      <div className="palco">
        <section className="bloco destaque">
          <h2>Evolução do mês</h2>
          <LinhaEvolucao transacoes={transacoesMes} ano={ano} mes={mes} />
        </section>

        <div className="coluna-apoio">
          <section className="bloco">
            <h2>Pra onde foi</h2>
            <Donut dados={relatorio} total={totalGasto} />
          </section>

          <section className="bloco">
            <h2>Entradas × Saídas</h2>
            <BarrasComparativo entradas={entradas} saidas={saidas} />
          </section>
        </div>
      </div>

      {/* ---- últimos lançamentos ---- */}
      <section className="bloco">
        <h2>Últimos lançamentos</h2>
        {recentes.length === 0 ? (
          <p className="vazio">Nada lançado ainda.</p>
        ) : (
          <ul className="recentes">
            {recentes.slice(0, 8).map((t) => {
              const cat = categorias.find((c) => c.id === t.categoryId);
              return (
                <li key={t.id} onClick={() => onVerConta(t.accountId)}>
                  <i className="ponto" style={{ background: cat?.cor ?? 'var(--text-faint)' }} />
                  <span className="rec-desc">
                    {t.descricao || '(sem descrição)'}
                    {t.parcelaNum != null && (
                      <em className="badge">{t.parcelaNum}/{t.parcelaTotal}</em>
                    )}
                  </span>
                  <span className="rec-conta">{nomeConta(t.accountId)}</span>
                  <span className="rec-data">{formatDataCurta(t.data)}</span>
                  <span className={t.valorCents < 0 ? 'ext-valor negativo' : 'ext-valor positivo'}>
                    {formatBRL(t.valorCents)}
                  </span>
                </li>
              );
            })}
          </ul>
        )}
      </section>
    </>
  );
}
