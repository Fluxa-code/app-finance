import { useEffect, useState, type FormEvent } from 'react';
import { api, API_URL, getNome, limparSessao, NaoAutenticado } from './api';
import './App.css';

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

function formatBRL(cents: number): string {
  return (cents / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function formatData(iso: string): string {
  return iso.split('-').reverse().join('/');
}

export default function Painel({ aoSair }: { aoSair: () => void }) {
  const [contas, setContas] = useState<Conta[]>([]);
  const [saldos, setSaldos] = useState<Saldo[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [tick, setTick] = useState(0);

  const [contaSel, setContaSel] = useState<string | null>(null);
  const [extrato, setExtrato] = useState<Transacao[]>([]);

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

  // formulário de NOVA CONTA
  const [mostrarNovaConta, setMostrarNovaConta] = useState(false);
  const [ncNome, setNcNome] = useState('');
  const [ncTipo, setNcTipo] = useState('CORRENTE');
  const [ncSaldo, setNcSaldo] = useState('');
  const [ncLimite, setNcLimite] = useState('');
  const [ncFechamento, setNcFechamento] = useState('');
  const [ncVencimento, setNcVencimento] = useState('');
  const [ncMsg, setNcMsg] = useState<string | null>(null);
  const [ncEnviando, setNcEnviando] = useState(false);

  // sessão venceu no meio do uso? volta pro login
  function tratarFalha(err: unknown) {
    if (err instanceof NaoAutenticado) {
      aoSair();
      return true;
    }
    return false;
  }

  useEffect(() => {
    async function carregar() {
      try {
        const contasResp = await api.get<Conta[]>('/contas');
        setContas(contasResp);
        setContaId((atual) => atual || (contasResp[0]?.id ?? ''));
        setContaDestino((atual) => atual || (contasResp[1]?.id ?? ''));

        const saldosResp = await Promise.all(
          contasResp.map((c) => api.get<Saldo>(`/contas/${c.id}/saldo`)),
        );
        setSaldos(saldosResp);
        setErro(null);
      } catch (err) {
        if (!tratarFalha(err)) {
          setErro('Não consegui falar com a API. O backend está rodando?');
        }
      } finally {
        setCarregando(false);
      }
    }
    carregar();
  }, [tick]);

  useEffect(() => {
    if (!contaSel) {
      setExtrato([]);
      return;
    }
    api
      .get<Transacao[]>(`/transacoes/conta/${contaSel}`)
      .then(setExtrato)
      .catch((err) => {
        if (!tratarFalha(err)) setExtrato([]);
      });
  }, [contaSel, tick]);

  // tempo real (SSE) — endpoint aberto, só avisa "algo mudou"
  useEffect(() => {
    const es = new EventSource(`${API_URL}/eventos`);
    es.addEventListener('mudanca', () => setTick((t) => t + 1));
    return () => es.close();
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

    let caminho = '/transacoes';
    let body: Record<string, unknown>;

    if (modo === 'SIMPLES') {
      body = { id: crypto.randomUUID(), accountId: contaId, tipo, valorCents, descricao, data };
    } else if (modo === 'TRANSFERENCIA') {
      if (contaId === contaDestino) {
        setMsgForm('Origem e destino não podem ser a mesma conta.');
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
    } else {
      const n = Number(parcelas);
      if (!Number.isInteger(n) || n < 2) {
        setMsgForm('Parcelamento precisa de 2 parcelas ou mais.');
        return;
      }
      caminho = '/transacoes/parcelamentos';
      body = {
        id: crypto.randomUUID(),
        accountId: contaId,
        descricao,
        valorTotalCents: valorCents,
        parcelaTotal: n,
        dataPrimeira: data,
      };
    }

    setEnviando(true);
    try {
      await api.post(caminho, body);
      setValor('');
      setDescricao('');
      setMsgForm('Lançado! ✅');
      setTick((t) => t + 1);
    } catch (err) {
      if (!tratarFalha(err)) {
        setMsgForm(err instanceof Error ? err.message : 'Erro de conexão.');
      }
    } finally {
      setEnviando(false);
    }
  }

  // "45,90" ou "-120" -> centavos. null = campo vazio. Aceita negativo
  // (conta pode nascer no vermelho), diferente do parse dos lançamentos.
  function parseReais(txt: string): number | null {
    if (!txt.trim()) return null;
    const n = Number(txt.replace(',', '.'));
    return Number.isFinite(n) ? Math.round(n * 100) : null;
  }

  async function criarConta(e: FormEvent) {
    e.preventDefault();
    setNcMsg(null);

    if (!ncNome.trim()) {
      setNcMsg('Dá um nome pra conta.');
      return;
    }

    const cartao = ncTipo === 'CARTAO_CREDITO';
    if (cartao && !ncFechamento) {
      setNcMsg('Cartão de crédito precisa do dia de fechamento.');
      return;
    }

    const body: Record<string, unknown> = {
      id: crypto.randomUUID(),
      nome: ncNome.trim(),
      tipo: ncTipo,
      saldoInicialCents: parseReais(ncSaldo) ?? 0,
    };
    if (cartao) {
      body.limiteCents = parseReais(ncLimite) ?? undefined; // undefined some do JSON
      body.diaFechamento = Number(ncFechamento);
      if (ncVencimento) body.diaVencimento = Number(ncVencimento);
    }

    setNcEnviando(true);
    try {
      await api.post('/contas', body);
      setNcNome('');
      setNcSaldo('');
      setNcLimite('');
      setNcFechamento('');
      setNcVencimento('');
      setMostrarNovaConta(false);
      setTick((t) => t + 1);
    } catch (err) {
      if (!tratarFalha(err)) {
        setNcMsg(err instanceof Error ? err.message : 'Erro de conexão.');
      }
    } finally {
      setNcEnviando(false);
    }
  }

  if (carregando) return <p className="status">Carregando…</p>;
  if (erro) return <p className="status erro">{erro}</p>;

  const total = saldos.reduce((soma, s) => soma + s.saldoCents, 0);
  const contaSelNome = saldos.find((s) => s.contaId === contaSel)?.nome;

  return (
    <main className="app">
      <div className="topo-linha">
        <div>
          <h1>Fluxa</h1>
          <p className="subtitulo">Minhas contas — clique numa conta pra ver o extrato</p>
        </div>
        <div className="usuario">
          <span>{getNome()}</span>
          <button
            className="sair"
            onClick={() => {
              limparSessao();
              aoSair();
            }}
          >
            Sair
          </button>
        </div>
      </div>

      {saldos.length === 0 && (
        <p className="vazio">Bem-vindo! Crie sua primeira conta no botão abaixo. 👇</p>
      )}

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

        <button
          type="button"
          className="card nova-conta"
          onClick={() => setMostrarNovaConta((v) => !v)}
        >
          + Nova conta
        </button>
      </section>

      {mostrarNovaConta && (
        <form className="form form-conta" onSubmit={criarConta}>
          <h2 className="form-titulo">Nova conta</h2>

          <div className="linha">
            <label>
              Nome
              <input
                value={ncNome}
                onChange={(e) => setNcNome(e.target.value)}
                placeholder="Nubank, Carteira…"
              />
            </label>
            <label>
              Tipo
              <select value={ncTipo} onChange={(e) => setNcTipo(e.target.value)}>
                <option value="CORRENTE">Conta corrente</option>
                <option value="POUPANCA">Poupança</option>
                <option value="CARTEIRA">Carteira</option>
                <option value="CARTAO_CREDITO">Cartão de crédito</option>
              </select>
            </label>
          </div>

          <div className="linha">
            <label>
              Saldo inicial (R$)
              <input
                inputMode="decimal"
                placeholder="0,00"
                value={ncSaldo}
                onChange={(e) => setNcSaldo(e.target.value)}
              />
            </label>
            {ncTipo === 'CARTAO_CREDITO' && (
              <label>
                Limite (R$)
                <input
                  inputMode="decimal"
                  placeholder="0,00"
                  value={ncLimite}
                  onChange={(e) => setNcLimite(e.target.value)}
                />
              </label>
            )}
          </div>

          {ncTipo === 'CARTAO_CREDITO' && (
            <div className="linha">
              <label>
                Dia de fechamento
                <input
                  type="number"
                  min={1}
                  max={31}
                  value={ncFechamento}
                  onChange={(e) => setNcFechamento(e.target.value)}
                />
              </label>
              <label>
                Dia de vencimento
                <input
                  type="number"
                  min={1}
                  max={31}
                  value={ncVencimento}
                  onChange={(e) => setNcVencimento(e.target.value)}
                />
              </label>
            </div>
          )}

          <button type="submit" disabled={ncEnviando}>
            {ncEnviando ? 'Criando…' : 'Criar conta'}
          </button>

          {ncMsg && <p className="msg">{ncMsg}</p>}
        </form>
      )}

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
            onClick={() => {
              setModo('SIMPLES');
              setMsgForm(null);
            }}
          >
            Gasto / Receita
          </button>
          <button
            type="button"
            className={modo === 'TRANSFERENCIA' ? 'aba ativa' : 'aba'}
            onClick={() => {
              setModo('TRANSFERENCIA');
              setMsgForm(null);
            }}
          >
            Transferência
          </button>
          <button
            type="button"
            className={modo === 'PARCELAMENTO' ? 'aba ativa' : 'aba'}
            onClick={() => {
              setModo('PARCELAMENTO');
              setMsgForm(null);
            }}
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
                  <option key={c.id} value={c.id}>
                    {c.nome}
                  </option>
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
                  <option key={c.id} value={c.id}>
                    {c.nome}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Para
              <select value={contaDestino} onChange={(e) => setContaDestino(e.target.value)}>
                {contas.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.nome}
                  </option>
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
                  <option key={c.id} value={c.id}>
                    {c.nome}
                  </option>
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
