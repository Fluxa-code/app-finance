import { useState, type FormEvent } from 'react';
import { api, salvarSessao } from './api';
import { useTema } from './useTema';
import './App.css';

type TokenResponse = { token: string; expiraEm: string; nome: string };

export default function Login({ aoEntrar }: { aoEntrar: () => void }) {
  useTema(); // aplica o tema salvo/do sistema já na tela de entrada
  const [modo, setModo] = useState<'LOGIN' | 'REGISTRO'>('LOGIN');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [nome, setNome] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function enviar(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    setEnviando(true);
    try {
      const resp =
        modo === 'LOGIN'
          ? await api.post<TokenResponse>('/auth/login', { email, senha })
          : await api.post<TokenResponse>('/auth/registrar', { email, senha, nome });

      salvarSessao(resp.token, resp.nome);
      aoEntrar(); // avisa o App: "logou, mostra o painel"
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro inesperado.');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main className="app login-tela">
      <div className="marca">
        <div className="logo">K</div>
      </div>
      <h1>Kofre</h1>
      <p className="subtitulo">
        {modo === 'LOGIN' ? 'Entre na sua conta' : 'Crie sua conta'}
      </p>

      <form className="form" onSubmit={enviar}>
        {modo === 'REGISTRO' && (
          <label>
            Nome
            <input value={nome} onChange={(e) => setNome(e.target.value)} />
          </label>
        )}

        <label>
          E-mail
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
        </label>

        <label>
          Senha
          <input
            type="password"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            placeholder={modo === 'REGISTRO' ? 'mínimo 8 caracteres' : ''}
          />
        </label>

        <button type="submit" disabled={enviando}>
          {enviando ? 'Aguarde…' : modo === 'LOGIN' ? 'Entrar' : 'Criar conta'}
        </button>

        {erro && <p className="msg">{erro}</p>}
      </form>

      <button
        className="alternar"
        onClick={() => {
          setModo((m) => (m === 'LOGIN' ? 'REGISTRO' : 'LOGIN'));
          setErro(null);
        }}
      >
        {modo === 'LOGIN' ? 'Não tem conta? Criar conta' : 'Já tem conta? Entrar'}
      </button>

      <p className="rodape">um produto Fluxa Labs</p>
    </main>
  );
}
