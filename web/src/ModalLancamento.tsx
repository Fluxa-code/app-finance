import { useEffect, useState, type FormEvent } from 'react';
import { api, NaoAutenticado } from './api';
import { DIAS_SEMANA, MESES, type Categoria, type Conta } from './tipos';

type Modo = 'SIMPLES' | 'TRANSFERENCIA' | 'PARCELAMENTO' | 'RECORRENCIA';

type Props = {
  contas: Conta[];
  categorias: Categoria[];
  contaPadrao?: string;
  aoFechar: () => void;
  aoLancar: () => void;
  aoDeslogar: () => void;
};

export default function ModalLancamento({
  contas,
  categorias,
  contaPadrao,
  aoFechar,
  aoLancar,
  aoDeslogar,
}: Props) {
  const [modo, setModo] = useState<Modo>('SIMPLES');
  const [tipo, setTipo] = useState<'SAIDA' | 'ENTRADA'>('SAIDA');
  const [contaId, setContaId] = useState(contaPadrao ?? contas[0]?.id ?? '');
  const [contaDestino, setContaDestino] = useState(contas[1]?.id ?? '');
  const [categoriaId, setCategoriaId] = useState('');
  const [valor, setValor] = useState('');
  const [parcelas, setParcelas] = useState('2');
  const [descricao, setDescricao] = useState('');
  const [data, setData] = useState(() => new Date().toISOString().slice(0, 10));
  const [enviando, setEnviando] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  // recorrência
  const [freq, setFreq] = useState<'MENSAL' | 'SEMANAL' | 'ANUAL'>('MENSAL');
  const [diaRec, setDiaRec] = useState(String(new Date().getDate()));
  const [mesRec, setMesRec] = useState(String(new Date().getMonth() + 1));
  const [dataFim, setDataFim] = useState('');

  // Esc fecha o modal — atalho que todo mundo espera
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && aoFechar();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [aoFechar]);

  async function enviar(e: FormEvent) {
    e.preventDefault();
    setMsg(null);

    const valorReais = Number(valor.replace(',', '.'));
    if (!valorReais || valorReais <= 0) {
      setMsg('Informe um valor maior que zero.');
      return;
    }
    const valorCents = Math.round(valorReais * 100);

    let caminho = '/transacoes';
    let body: Record<string, unknown>;

    if (modo === 'SIMPLES') {
      body = {
        id: crypto.randomUUID(),
        accountId: contaId,
        categoryId: categoriaId || undefined,
        tipo,
        valorCents,
        descricao,
        data,
      };
    } else if (modo === 'TRANSFERENCIA') {
      if (contaId === contaDestino) {
        setMsg('Origem e destino não podem ser a mesma conta.');
        return;
      }
      caminho = '/transacoes/transferencias';
      body = {
        transferId: crypto.randomUUID(),
        origemId: crypto.randomUUID(),
        destinoId: crypto.randomUUID(),
        contaOrigemId: contaId,
        contaDestinoId: contaDestino,
        valorCents,
        descricao,
        data,
      };
    } else if (modo === 'PARCELAMENTO') {
      const n = Number(parcelas);
      if (!Number.isInteger(n) || n < 2) {
        setMsg('Parcelamento precisa de 2 parcelas ou mais.');
        return;
      }
      caminho = '/transacoes/parcelamentos';
      body = {
        id: crypto.randomUUID(),
        accountId: contaId,
        categoryId: categoriaId || undefined,
        descricao,
        valorTotalCents: valorCents,
        parcelaTotal: n,
        dataPrimeira: data,
      };
    } else {
      if (!descricao.trim()) {
        setMsg('Recorrência precisa de descrição (ex.: Salário, Aluguel).');
        return;
      }
      caminho = '/recorrencias';
      body = {
        id: crypto.randomUUID(),
        accountId: contaId,
        categoryId: categoriaId || undefined,
        tipo,
        valorCents,
        descricao: descricao.trim(),
        frequencia: freq,
        dia: Number(diaRec),
        mes: freq === 'ANUAL' ? Number(mesRec) : undefined,
        dataInicio: data,
        dataFim: dataFim || undefined,
      };
    }

    setEnviando(true);
    try {
      await api.post(caminho, body);
      aoLancar();
      aoFechar();
    } catch (err) {
      if (err instanceof NaoAutenticado) {
        aoDeslogar();
        return;
      }
      setMsg(err instanceof Error ? err.message : 'Erro de conexão.');
    } finally {
      setEnviando(false);
    }
  }

  const catsFiltradas = categorias.filter((c) =>
    modo === 'PARCELAMENTO'
      ? c.tipo === 'DESPESA'
      : c.tipo === (tipo === 'ENTRADA' ? 'RECEITA' : 'DESPESA'),
  );

  return (
    // clicar no fundo escuro fecha; clicar no card não (stopPropagation)
    <div className="overlay" onClick={aoFechar}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-topo">
          <h2>{modo === 'RECORRENCIA' ? 'Nova recorrência' : 'Novo lançamento'}</h2>
          <button className="btn-icone" onClick={aoFechar} title="Fechar (Esc)">✕</button>
        </div>

        <form className="form" onSubmit={enviar}>
          <div className="abas">
            {(
              [
                ['SIMPLES', 'Gasto / Receita'],
                ['TRANSFERENCIA', 'Transferência'],
                ['PARCELAMENTO', 'Parcelamento'],
                ['RECORRENCIA', '🔁 Recorrente'],
              ] as [Modo, string][]
            ).map(([m, rotulo]) => (
              <button
                key={m}
                type="button"
                className={modo === m ? 'aba ativa' : 'aba'}
                onClick={() => { setModo(m); setMsg(null); }}
              >
                {rotulo}
              </button>
            ))}
          </div>

          {(modo === 'SIMPLES' || modo === 'RECORRENCIA') && (
            <div className="linha">
              <label>
                Tipo
                <select value={tipo} onChange={(e) => setTipo(e.target.value as 'SAIDA' | 'ENTRADA')}>
                  <option value="SAIDA">Gasto</option>
                  <option value="ENTRADA">Receita</option>
                </select>
              </label>
              <label>
                Conta
                <select value={contaId} onChange={(e) => setContaId(e.target.value)}>
                  {contas.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
                </select>
              </label>
            </div>
          )}

          {modo === 'TRANSFERENCIA' && (
            <div className="linha">
              <label>
                De
                <select value={contaId} onChange={(e) => setContaId(e.target.value)}>
                  {contas.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
                </select>
              </label>
              <label>
                Para
                <select value={contaDestino} onChange={(e) => setContaDestino(e.target.value)}>
                  {contas.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
                </select>
              </label>
            </div>
          )}

          {modo === 'PARCELAMENTO' && (
            <div className="linha">
              <label>
                Conta
                <select value={contaId} onChange={(e) => setContaId(e.target.value)}>
                  {contas.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
                </select>
              </label>
              <label>
                Nº de parcelas
                <input type="number" min={2} max={48} value={parcelas}
                       onChange={(e) => setParcelas(e.target.value)} />
              </label>
            </div>
          )}

          {modo === 'RECORRENCIA' && (
            <>
              <div className="linha">
                <label>
                  Repete
                  <select value={freq} onChange={(e) => setFreq(e.target.value as typeof freq)}>
                    <option value="MENSAL">Todo mês</option>
                    <option value="SEMANAL">Toda semana</option>
                    <option value="ANUAL">Todo ano</option>
                  </select>
                </label>
                {freq === 'SEMANAL' ? (
                  <label>
                    Dia da semana
                    <select value={diaRec} onChange={(e) => setDiaRec(e.target.value)}>
                      {DIAS_SEMANA.map((d, i) => <option key={d} value={i + 1}>{d}</option>)}
                    </select>
                  </label>
                ) : (
                  <label>
                    Dia do mês
                    <input type="number" min={1} max={31} value={diaRec}
                           onChange={(e) => setDiaRec(e.target.value)} />
                  </label>
                )}
                {freq === 'ANUAL' && (
                  <label>
                    Mês
                    <select value={mesRec} onChange={(e) => setMesRec(e.target.value)}>
                      {MESES.map((m, i) => <option key={m} value={i + 1}>{m}</option>)}
                    </select>
                  </label>
                )}
              </div>
              <p className="dica">
                Dia 29, 30 ou 31 em mês curto cai no último dia do mês.
              </p>
            </>
          )}

          <div className="linha">
            <label>
              {modo === 'PARCELAMENTO' ? 'Valor total (R$)' : 'Valor (R$)'}
              <input inputMode="decimal" placeholder="0,00" value={valor} autoFocus
                     onChange={(e) => setValor(e.target.value)} />
            </label>
            <label>
              {modo === 'PARCELAMENTO' ? 'Data da 1ª parcela'
                : modo === 'RECORRENCIA' ? 'Começa em'
                : 'Data'}
              <input type="date" value={data} onChange={(e) => setData(e.target.value)} />
            </label>
            {modo === 'RECORRENCIA' && (
              <label>
                Termina em <span className="opcional">(opcional)</span>
                <input type="date" value={dataFim} onChange={(e) => setDataFim(e.target.value)} />
              </label>
            )}
          </div>

          <div className="linha">
            <label>
              Descrição
              <input value={descricao} onChange={(e) => setDescricao(e.target.value)}
                     placeholder="Mercado, uber, geladeira 10x…" />
            </label>
            {modo !== 'TRANSFERENCIA' && (
              <label>
                Categoria
                <select value={categoriaId} onChange={(e) => setCategoriaId(e.target.value)}>
                  <option value="">— sem categoria —</option>
                  {catsFiltradas.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
                </select>
              </label>
            )}
          </div>

          <button type="submit" disabled={enviando}>
            {enviando ? 'Salvando…'
              : modo === 'TRANSFERENCIA' ? 'Transferir'
              : modo === 'RECORRENCIA' ? 'Criar recorrência'
              : 'Lançar'}
          </button>

          {msg && <p className="msg">{msg}</p>}
        </form>
      </div>
    </div>
  );
}
