import { useEffect, useState, type FormEvent } from 'react';
import './App.css';

const API = 'http://localhost:8080';
const USER_ID = '11111111-1111-1111-1111-111111111111'; // temporário — até ter login

type Conta = { id: string; nome: string; tipo: string };
type Saldo = { contaId: string; nome: string; saldoCents: number };
type Transacao = {
  id: string;
  descricao: string | null;
  valorCents: number;
  data: string;
  tipo: string;
  parcelaNum: number | null;
  parcelaTotal: number | null;
};

type Modo = 'SIMPLES' | 'TRANSFERENCIA' | 'PARCELAMENTO';

const TIPO_LABEL: Record<string, string> = {
  CORRENTE: 'Conta corrente',
  POUPANCA: 'Poupança',
  CARTEIRA: 'Carteira',
  CARTAO_CREDITO: 'Cartão de crédito',
};

// centavos (inteiro) -> "R$ 1.234,56"
function formatBRL(cents: number): string {
  return (cents / 100).toLocaleString('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  });
}

// "2026-08-15" -> "15/08/2026"
function formatData(iso: string): string {
  return iso.split('-').reverse().join('/');
}

export default function App() {
  const [contas, setContas] = useState<Conta[]>([]);
  const [saldos, setSaldos] = useState<Saldo[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  // tick: contador de recarga. Qualquer mudança (minha ou de OUTRA tela via SSE)
  // incrementa ele, e os efeitos abaixo recarregam os dados.
  const [tick, setTick] = useState(0);

  // extrato da conta clicada
  const [contaSel, setContaSel] = useState<string | null>(null);
  const [extrato, setExtrato] = useState<Transacao[]>([]);

  // formulário
  const [modo, setModo] = useState<Modo>('SIMPLES');
  const [tipo, setTipo] = useState<'SAIDA' | 'ENTRADA'>('SAIDA');
  const [contaId, setContaId] = useState('');
  const [contaDestino, setContaDestino] = useState('');
  const [valor, setValor] = useState('');
  const [parcelas, setParcelas] = useState('2');
  const [descricao, setDescricao] = useState('');
  const [data, setData] = useState(() => new Date().toISOString().slice(0, 10));
  const [enviando, setEnviando] = useState(false);
  const [msgForm, setMsgForm] = useState<string | null>(null);

  // contas + saldos — roda na abertura e a cada tick
  useEffect(() => {
    async function carregar() {
      try {
        const contasResp: Conta[] = await fetch(`${API}/contas`).then((r) => r.json());
        setContas(contasResp);
        // só define os selects na primeira carga; não sobrescreve escolha do usuário
        setContaId((atual) => atual || (contasResp[0]?.id ?? ''));
        setContaDestino((atual) => atual || (contasResp[1]?.id ?? ''));

        const saldosResp: Saldo[] = await Promise.all(
          contasResp.map((c) => fetch(`${API}/contas/${c.id}/saldo`).then((r) => r.json())),
        );
        setSaldos(saldosResp);
        setErro(null);
      } catch {
        setErro('Não consegui falar com a API. O backend está rodando?');
      } finally {
        setCarregando(false);
      }
    }
    carregar();
  }, [tick]);

  // extrato da conta selecionada — recarrega se trocar de conta OU se algo mudar (tick)
  useEffect(() => {
    if (!contaSel) {
      setExtrato([]);
      return;
    }
    fetch(`${API}/transacoes/conta/${contaSel}`)
      .then((r) => r.json())
      .then(setExtrato)
      .catch(() => setExtrato([]));
  }, [contaSel, tick]);

  // TEMPO REAL: conexão SSE aberta com o servidor. Qualquer lançamento
  // (nesta tela ou em outra) dispara "mudanca" -> tick -> tudo recarrega.
  useEffect(() => {
    const es = new EventSource(`${API}/eventos`);
    es.addEventListener('mudanca', () => setTick((t) => t + 1));
    return () => es.close(); // fecha a conexão se o componente sair da tela
  }, []);

  async function enviar(e: FormEvent) {
    e.preventDefault();
    setMsgForm(null);

    const valorReais = Number(valor.replace(',', '.'));
    if (!valorReais || valorReais <= 0) {
      setMsgForm('Informe um valor maior que zero.');
      return;
    }
    const valorCents = Math.round(valorReais * 100);

    let url = `${API}/transacoes`;
    let body: Record<string, unknown>;

    if (modo === 'SIMPLES') {
      body = {
        id: crypto.randomUUID(),
        userId: USER_ID,
        accountId: contaId,
        tipo,
        valorCents,
        descricao,
        data,
      };
    } else if (modo === 'TRANSFERENCIA') {
      if (contaId === contaDestino) {
        setMsgForm('Origem e destino não podem ser a mesma conta.');
        return;
      }
      url = `${API}/transacoes/transferencias`;
      body = {
        transferId: crypto.randomUUID(),
        origemId: crypto.randomUUID(),
        destinoId: crypto.randomUUID(),
        userId: USER_ID,
        contaOrigemId: contaId,
        contaDestinoId: contaDestino,
        valorCents,
        descricao,
        data,
      };
    } else {
      const n = Number(parcelas);
      if (!Number.isInteger(n) || n < 2) {
        setMsgForm('Parcelamento precisa de 2 parcelas ou mais.');
        return;
      }
      url = `${API}/transacoes/parcelamentos`;
      body = {
        id: crypto.randomUUID(),
        userId: USER_ID,
        accountId: contaId,
        descricao,
        valorTotalCents: valorCents,
        parcelaTotal: n,
        dataPrimeira: data,
      };
    }

    setEnviando(true);
    try {
      const resp = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });

      if (!resp.ok) {
        const err = await resp.json().catch(() => null);
        setMsgForm(err?.mensagem ?? 'Erro ao lançar.');
        return;
      }

      setValor('');
      setDescricao('');
      setMsgForm('Lançado! ✅');
      setTick((t) => t + 1); // recarrega saldos e extrato
    } catch {
      setMsgForm('Erro de conexão.');
    } finally {
      setEnviando(false);
    }
  }

  if (carregando) return <p className="status">Carregando…</p>;
  if (erro) return <p className="status erro">{erro}</p>;

  const total = saldos.reduce((soma, s) => soma + s.saldoCents, 0);
  const contaSelNome = saldos.find((s) => s.contaId === contaSel)?.nome;

  return (
    <main className="app">
      <h1>Fluxa</h1>
      <p className="subtitulo">Minhas contas — clique numa conta pra ver o extrato</p>

      <section className="cards">
        {saldos.map((s) => (
          <article
            className={contaSel === s.contaId ? 'card selecionado' : 'card'}
            key={s.contaId}
            onClick={() => setContaSel((sel) => (sel === s.contaId ? null : s.contaId))}
          >
            <span className="tipo">
              {TIPO_LABEL[contas.find((c) => c.id === s.contaId)?.tipo ?? ''] ?? ''}
            </span>
            <h2>{s.nome}</h2>
            <strong className={s.saldoCents < 0 ? 'saldo negativo' : 'saldo'}>
              {formatBRL(s.saldoCents)}
            </strong>
          </article>
        ))}
      </section>

      <footer className="total">
        Patrimônio total: <strong>{formatBRL(total)}</strong>
      </footer>

      {contaSel && (
        <section className="extrato">
          <h2>Extrato — {contaSelNome}</h2>
          {extrato.length === 0 ? (
            <p className="vazio">Nenhum lançamento nesta conta.</p>
          ) : (
            <ul>
              {extrato.map((t) => (
                <li key={t.id}>
                  <span className="ext-data">{formatData(t.data)}</span>
                  <span className="ext-desc">
                    {t.descricao || '(sem descrição)'}
                    {t.parcelaNum != null && (
                      <em className="badge">
                        {t.parcelaNum}/{t.parcelaTotal}
                      </em>
                    )}
                    {t.tipo === 'TRANSFERENCIA' && <em className="badge transf">transferência</em>}
                  </span>
                  <span className={t.valorCents < 0 ? 'ext-valor negativo' : 'ext-valor positivo'}>
                    {formatBRL(t.valorCents)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      <form className="form" onSubmit={enviar}>
        <div className="abas">
          <button
            type="button"
            className={modo === 'SIMPLES' ? 'aba ativa' : 'aba'}
            onClick={() => { setModo('SIMPLES'); setMsgForm(null); }}
          >
            Gasto / Receita
          </button>
          <button
            type="button"
            className={modo === 'TRANSFERENCIA' ? 'aba ativa' : 'aba'}
            onClick={() => { setModo('TRANSFERENCIA'); setMsgForm(null); }}
          >
            Transferência
          </button>
          <button
            type="button"
            className={modo === 'PARCELAMENTO' ? 'aba ativa' : 'aba'}
            onClick={() => { setModo('PARCELAMENTO'); setMsgForm(null); }}
          >
            Parcelamento
          </button>
        </div>

        {modo === 'SIMPLES' && (
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
                {contas.map((c) => (
                  <option key={c.id} value={c.id}>{c.nome}</option>
                ))}
              </select>
            </label>
          </div>
        )}

        {modo === 'TRANSFERENCIA' && (
          <div className="linha">
            <label>
              De
              <select value={contaId} onChange={(e) => setContaId(e.target.value)}>
                {contas.map((c) => (
                  <option key={c.id} value={c.id}>{c.nome}</option>
                ))}
              </select>
            </label>
            <label>
              Para
              <select value={contaDestino} onChange={(e) => setContaDestino(e.target.value)}>
                {contas.map((c) => (
                  <option key={c.id} value={c.id}>{c.nome}</option>
                ))}
              </select>
            </label>
          </div>
        )}

        {modo === 'PARCELAMENTO' && (
          <div className="linha">
            <label>
              Conta
              <select value={contaId} onChange={(e) => setContaId(e.target.value)}>
                {contas.map((c) => (
                  <option key={c.id} value={c.id}>{c.nome}</option>
                ))}
              </select>
            </label>
            <label>
              Nº de parcelas
              <input
                type="number"
                min={2}
                max={48}
                value={parcelas}
                onChange={(e) => setParcelas(e.target.value)}
              />
            </label>
          </div>
        )}

        <div className="linha">
          <label>
            {modo === 'PARCELAMENTO' ? 'Valor total (R$)' : 'Valor (R$)'}
            <input
              inputMode="decimal"
              placeholder="0,00"
              value={valor}
              onChange={(e) => setValor(e.target.value)}
            />
          </label>
          <label>
            {modo === 'PARCELAMENTO' ? 'Data da 1ª parcela' : 'Data'}
            <input type="date" value={data} onChange={(e) => setData(e.target.value)} />
          </label>
        </div>

        <label>
          Descrição
          <input
            value={descricao}
            onChange={(e) => setDescricao(e.target.value)}
            placeholder="Mercado, uber, geladeira 10x…"
          />
        </label>

        <button type="submit" disabled={enviando}>
          {enviando ? 'Lançando…' : modo === 'TRANSFERENCIA' ? 'Transferir' : 'Lançar'}
        </button>

        {msgForm && <p className="msg">{msgForm}</p>}
      </form>
    </main>
  );
}
