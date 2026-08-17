import { useEffect, useState } from 'react';
import './App.css';

const API = 'http://localhost:8080';

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

// rótulos bonitos pros tipos que vêm em MAIÚSCULO da API
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
  const [saldos, setSaldos] = useState<Saldo[]>([]);
  const [tipos, setTipos] = useState<Record<string, string>>({});
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  // roda uma vez, quando a tela abre
  useEffect(() => {
    async function carregar() {
      try {
        // 1) busca as contas
        const contas: Conta[] = await fetch(`${API}/contas`).then((r) => r.json());

        // guarda o tipo de cada conta (pra mostrar no card)
        const mapaTipos: Record<string, string> = {};
        contas.forEach((c) => (mapaTipos[c.id] = c.tipo));
        setTipos(mapaTipos);

        // 2) busca o saldo de cada conta (uma chamada por conta)
        const saldos: Saldo[] = await Promise.all(
          contas.map((c) => fetch(`${API}/contas/${c.id}/saldo`).then((r) => r.json())),
        );
        setSaldos(saldos);
      } catch {
        setErro('Não consegui falar com a API. O backend está rodando?');
      } finally {
        setCarregando(false);
      }
    }
    carregar();
  }, []);

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
            <span className="tipo">{TIPO_LABEL[tipos[s.contaId]] ?? tipos[s.contaId]}</span>
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
    </main>
  );
}
