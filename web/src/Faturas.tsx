import { useEffect, useState, type FormEvent } from 'react';
import { api, NaoAutenticado } from './api';
import { formatBRL, formatData, MESES, type Conta, type Fatura, type Transacao } from './tipos';

type Props = {
  cardId: string;
  contas: Conta[];
  tick: number;
  aoMudar: () => void;
  aoDeslogar: () => void;
};

const STATUS_LABEL = { ABERTA: 'Aberta', FECHADA: 'Fechada', PAGA: 'Paga' } as const;

export default function Faturas({ cardId, contas, tick, aoMudar, aoDeslogar }: Props) {
  const [faturas, setFaturas] = useState<Fatura[]>([]);
  const [aberta, setAberta] = useState<string | null>(null);   // fatura expandida
  const [itens, setItens] = useState<Transacao[]>([]);
  const [pagando, setPagando] = useState<Fatura | null>(null);
  const [pgConta, setPgConta] = useState('');
  const [pgValor, setPgValor] = useState('');
  const [pgData, setPgData] = useState(() => new Date().toISOString().slice(0, 10));
  const [pgMsg, setPgMsg] = useState<string | null>(null);

  const contasPagadoras = contas.filter((c) => c.tipo !== 'CARTAO_CREDITO');

  function falha(err: unknown) {
    if (err instanceof NaoAutenticado) { aoDeslogar(); return true; }
    return false;
  }

  useEffect(() => {
    api.get<Fatura[]>(`/faturas/cartao/${cardId}`)
      .then((f) => {
        setFaturas(f);
        // abre automaticamente a fatura mais relevante: a primeira não paga
        if (!aberta) setAberta(f.find((x) => x.status !== 'PAGA')?.id ?? f[0]?.id ?? null);
      })
      .catch((err) => { if (!falha(err)) setFaturas([]); });
  }, [cardId, tick]);

  useEffect(() => {
    if (!aberta) { setItens([]); return; }
    api.get<Transacao[]>(`/faturas/${aberta}/itens`)
      .then(setItens)
      .catch((err) => { if (!falha(err)) setItens([]); });
  }, [aberta, tick]);

  function abrirPagamento(f: Fatura) {
    setPagando(f);
    setPgConta(contasPagadoras[0]?.id ?? '');
    setPgValor((f.totalCents / 100).toFixed(2).replace('.', ','));
    setPgMsg(null);
  }

  async function pagar(e: FormEvent) {
    e.preventDefault();
    if (!pagando) return;
    const valor = Number(pgValor.replace(',', '.'));
    if (!valor || valor <= 0) return setPgMsg('Valor maior que zero.');
    if (!pgConta) return setPgMsg('Escolha a conta de origem.');
    try {
      await api.post(`/faturas/${pagando.id}/pagar`, {
        contaOrigemId: pgConta,
        valorCents: Math.round(valor * 100),
        data: pgData,
      });
      setPagando(null);
      aoMudar();
    } catch (err) {
      if (!falha(err)) setPgMsg(err instanceof Error ? err.message : 'Erro ao pagar.');
    }
  }

  if (faturas.length === 0) {
    return (
      <section className="bloco">
        <h2>Faturas</h2>
        <p className="vazio">Nenhuma fatura ainda. Lance uma compra neste cartão e ela aparece aqui.</p>
      </section>
    );
  }

  return (
    <section className="bloco">
      <h2>Faturas</h2>
      <ul className="faturas">
        {faturas.map((f) => {
          const expandida = aberta === f.id;
          return (
            <li key={f.id} className={`fatura ${f.status.toLowerCase()} ${expandida ? 'expandida' : ''}`}>
              <button className="fatura-cabecalho" onClick={() => setAberta(expandida ? null : f.id)}>
                <span className="fatura-mes">
                  {MESES[f.mes - 1]} {f.ano}
                  <em className={`badge status-${f.status.toLowerCase()}`}>{STATUS_LABEL[f.status]}</em>
                </span>
                <span className="fatura-datas">
                  fecha {formatData(f.dataFechamento)} · vence {formatData(f.dataVencimento)}
                </span>
                <strong className="fatura-total">{formatBRL(f.totalCents)}</strong>
                <span className="fatura-seta">{expandida ? '▾' : '▸'}</span>
              </button>

              {expandida && (
                <div className="fatura-corpo">
                  {f.status !== 'PAGA' && f.totalCents > 0 && (
                    pagando?.id === f.id ? (
                      <form className="form-pagar" onSubmit={pagar}>
                        <label>
                          Pagar com
                          <select value={pgConta} onChange={(e) => setPgConta(e.target.value)}>
                            {contasPagadoras.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
                          </select>
                        </label>
                        <label>
                          Valor (R$)
                          <input inputMode="decimal" value={pgValor} onChange={(e) => setPgValor(e.target.value)} />
                        </label>
                        <label>
                          Data
                          <input type="date" value={pgData} onChange={(e) => setPgData(e.target.value)} />
                        </label>
                        <div className="form-pagar-acoes">
                          <button type="submit" className="btn-ok">Confirmar</button>
                          <button type="button" className="btn-cancel" onClick={() => setPagando(null)}>Cancelar</button>
                        </div>
                        {pgMsg && <p className="msg-inline">{pgMsg}</p>}
                        <p className="dica">Pagar menos que o total é pagamento parcial — a fatura continua em aberto.</p>
                      </form>
                    ) : (
                      <button className="btn-novo compacto" onClick={() => abrirPagamento(f)}>
                        Pagar fatura
                      </button>
                    )
                  )}

                  {itens.length === 0 ? (
                    <p className="vazio">Sem lançamentos nesta fatura.</p>
                  ) : (
                    <ul className="fatura-itens">
                      {itens.map((t) => (
                        <li key={t.id}>
                          <span className="ext-data">{formatData(t.data)}</span>
                          <span className="ext-desc">
                            {t.descricao || '(sem descrição)'}
                            {t.parcelaNum != null && <em className="badge">{t.parcelaNum}/{t.parcelaTotal}</em>}
                            {t.tipo === 'TRANSFERENCIA' && <em className="badge transf">pagamento</em>}
                          </span>
                          <span className={t.valorCents < 0 ? 'ext-valor negativo' : 'ext-valor positivo'}>
                            {formatBRL(t.valorCents)}
                          </span>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
            </li>
          );
        })}
      </ul>
    </section>
  );
}
