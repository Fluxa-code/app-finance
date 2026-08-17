import { useEffect, useState, type FormEvent } from 'react';
import './App.css';

const API = 'http://localhost:8080';
const USER_ID = '11111111-1111-1111-1111-111111111111'; // temporário — até ter login

type Conta = {
  id: string;
  nome: string;
  tipo: string;
};

type Saldo = {
  contaId: string;
  nome: string;
  saldoCents: number;
};

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

export default function App() {
  const [contas, setContas] = useState<Conta[]>([]);
  const [saldos, setSaldos] = useState<Saldo[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  // estado do formulário
  const [contaId, setContaId] = useState('');
  const [tipo, setTipo] = useState<'SAIDA' | 'ENTRADA'>('SAIDA');
  const [valor, setValor] = useState('');
  const [descricao, setDescricao] = useState('');
  const [data, setData] = useState(() => new Date().toISOString().slice(0, 10)); // hoje
  const [enviando, setEnviando] = useState(false);
  const [msgForm, setMsgForm] = useState<string | null>(null);

  // busca contas + saldos. Dá pra chamar de novo depois de lançar, pra atualizar a tela.
  async function carregar() {
    const contas: Conta[] = await fetch(`${API}/contas`).then((r) => r.json());
    setContas(contas);
    if (!contaId && contas.length > 0) setContaId(contas[0].id);

    const saldos: Saldo[] = await Promise.all(
      contas.map((c) => fetch(`${API}/contas/${c.id}/saldo`).then((r) => r.json())),
    );
    setSaldos(saldos);
  }

  useEffect(() => {
    carregar()
      .catch(() => setErro('Não consegui falar com a API. O backend está rodando?'))
      .finally(() => setCarregando(false));
  }, []);

  async function lancar(e: FormEvent) {
    e.preventDefault(); // impede o navegador de recarregar a página
    setMsgForm(null);

    // "45,90" ou "45.90" -> número
    const valorReais = Number(valor.replace(',', '.'));
    if (!contaId || !valorReais || valorReais <= 0) {
      setMsgForm('Escolha uma conta e um valor maior que zero.');
      return;
    }
    const valorCents = Math.round(valorReais * 100); // reais -> centavos inteiros

    setEnviando(true);
    try {
      const resp = await fetch(`${API}/transacoes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          id: crypto.randomUUID(), // id gerado no cliente (offline-first)
          userId: USER_ID,
          accountId: contaId,
          tipo, // o backend aplica o sinal (SAIDA vira negativo)
          valorCents, // sempre positivo aqui
          descricao,
          data,
        }),
      });

      if (!resp.ok) {
        const err = await resp.json();
        setMsgForm(err.mensagem ?? 'Erro ao lançar.');
        return;
      }

      // deu certo: limpa o form e recarrega os saldos (a tela atualiza sozinha)
      setValor('');
      setDescricao('');
      setMsgForm('Lançado! ✅');
      await carregar();
    } catch {
      setMsgForm('Erro de conexão.');
    } finally {
      setEnviando(false);
    }
  }

  if (carregando) return <p className="status">Carregando…</p>;
  if (erro) return <p className="status erro">{erro}</p>;

  const total = saldos.reduce((soma, s) => soma + s.saldoCents, 0);

  return (
    <main className="app">
      <h1>Fluxa</h1>
      <p className="subtitulo">Minhas contas</p>

      <section className="cards">
        {saldos.map((s) => (
          <article className="card" key={s.contaId}>
            <span className="tipo">{TIPO_LABEL[tipos(contas, s.contaId)] ?? tipos(contas, s.contaId)}</span>
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

      <form className="form" onSubmit={lancar}>
        <h2 className="form-titulo">Lançar</h2>

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

        <div className="linha">
          <label>
            Valor (R$)
            <input
              inputMode="decimal"
              placeholder="0,00"
              value={valor}
              onChange={(e) => setValor(e.target.value)}
            />
          </label>

          <label>
            Data
            <input type="date" value={data} onChange={(e) => setData(e.target.value)} />
          </label>
        </div>

        <label>
          Descrição
          <input
            value={descricao}
            onChange={(e) => setDescricao(e.target.value)}
            placeholder="Mercado, uber…"
          />
        </label>

        <button type="submit" disabled={enviando}>
          {enviando ? 'Lançando…' : 'Lançar'}
        </button>

        {msgForm && <p className="msg">{msgForm}</p>}
      </form>
    </main>
  );
}

// acha o tipo de uma conta pelo id
function tipos(contas: Conta[], id: string): string {
  return contas.find((c) => c.id === id)?.tipo ?? '';
}
