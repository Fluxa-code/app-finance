import { useEffect, useState, type FormEvent } from 'react';
import { api, limparSessao, NaoAutenticado, salvarSessao, getToken } from './api';
import { formatData } from './tipos';

type Perfil = { id: string; email: string; nome: string; membroDesde: string };

export default function PerfilTela({ aoSair }: { aoSair: () => void }) {
  const [perfil, setPerfil] = useState<Perfil | null>(null);
  const [nome, setNome] = useState('');
  const [msgNome, setMsgNome] = useState<string | null>(null);

  const [senhaAtual, setSenhaAtual] = useState('');
  const [novaSenha, setNovaSenha] = useState('');
  const [confirma, setConfirma] = useState('');
  const [msgSenha, setMsgSenha] = useState<string | null>(null);

  const [confirmandoExclusao, setConfirmandoExclusao] = useState(false);
  const [senhaExclusao, setSenhaExclusao] = useState('');
  const [textoConfirma, setTextoConfirma] = useState('');
  const [msgExclusao, setMsgExclusao] = useState<string | null>(null);
  const [excluindo, setExcluindo] = useState(false);

  function falha(err: unknown) {
    if (err instanceof NaoAutenticado) { aoSair(); return true; }
    return false;
  }

  useEffect(() => {
    api.get<Perfil>('/me')
      .then((p) => { setPerfil(p); setNome(p.nome); })
      .catch(falha);
  }, []);

  async function salvarNome(e: FormEvent) {
    e.preventDefault();
    setMsgNome(null);
    if (!nome.trim()) return setMsgNome('O nome não pode ficar vazio.');
    try {
      const p = await api.put<Perfil>('/me', { nome: nome.trim() });
      setPerfil(p);
      // atualiza o nome que aparece na sidebar (está no localStorage junto do token)
      const token = getToken();
      if (token) salvarSessao(token, p.nome);
      setMsgNome('Nome atualizado ✅');
    } catch (err) {
      if (!falha(err)) setMsgNome(err instanceof Error ? err.message : 'Erro.');
    }
  }

  async function trocarSenha(e: FormEvent) {
    e.preventDefault();
    setMsgSenha(null);
    if (novaSenha !== confirma) return setMsgSenha('A confirmação não bate com a nova senha.');
    if (novaSenha.length < 8) return setMsgSenha('A nova senha precisa ter pelo menos 8 caracteres.');
    try {
      await api.putVazio('/me/senha', { senhaAtual, novaSenha });
      setSenhaAtual(''); setNovaSenha(''); setConfirma('');
      setMsgSenha('Senha alterada ✅');
    } catch (err) {
      if (!falha(err)) setMsgSenha(err instanceof Error ? err.message : 'Erro.');
    }
  }

  async function excluirConta(e: FormEvent) {
    e.preventDefault();
    setMsgExclusao(null);
    if (textoConfirma !== 'EXCLUIR') return setMsgExclusao('Digite EXCLUIR (em maiúsculas) pra confirmar.');
    setExcluindo(true);
    try {
      await api.del('/me', { senha: senhaExclusao });
      limparSessao();
      aoSair();   // conta não existe mais → volta pro login
    } catch (err) {
      if (!falha(err)) setMsgExclusao(err instanceof Error ? err.message : 'Erro.');
      setExcluindo(false);
    }
  }

  if (!perfil) return <p className="status">Carregando…</p>;

  return (
    <>
      <header className="cabecalho">
        <div>
          <span className="cab-tipo">Sua conta</span>
          <h2>Perfil</h2>
        </div>
      </header>

      {/* ---- dados ---- */}
      <section className="bloco">
        <h2>Dados</h2>
        <form className="form" onSubmit={salvarNome} style={{ marginTop: '1rem' }}>
          <div className="linha">
            <label>
              Nome
              <input value={nome} onChange={(e) => setNome(e.target.value)} />
            </label>
            <label>
              E-mail
              <input value={perfil.email} disabled title="E-mail não pode ser alterado por enquanto" />
            </label>
          </div>
          <p className="dica">Membro desde {formatData(perfil.membroDesde.slice(0, 10))}</p>
          <div className="form-pagar-acoes">
            <button type="submit" className="btn-ok">Salvar</button>
          </div>
          {msgNome && <p className="msg">{msgNome}</p>}
        </form>
      </section>

      {/* ---- senha ---- */}
      <section className="bloco">
        <h2>Trocar senha</h2>
        <form className="form" onSubmit={trocarSenha} style={{ marginTop: '1rem' }}>
          <div className="linha">
            <label>
              Senha atual
              <input type="password" value={senhaAtual} onChange={(e) => setSenhaAtual(e.target.value)}
                     autoComplete="current-password" />
            </label>
          </div>
          <div className="linha">
            <label>
              Nova senha
              <input type="password" value={novaSenha} onChange={(e) => setNovaSenha(e.target.value)}
                     autoComplete="new-password" placeholder="mínimo 8 caracteres" />
            </label>
            <label>
              Confirmar nova senha
              <input type="password" value={confirma} onChange={(e) => setConfirma(e.target.value)}
                     autoComplete="new-password" />
            </label>
          </div>
          <div className="form-pagar-acoes">
            <button type="submit" className="btn-ok">Alterar senha</button>
          </div>
          {msgSenha && <p className="msg">{msgSenha}</p>}
        </form>
      </section>

      {/* ---- zona de perigo ---- */}
      <section className="bloco perigo">
        <h2>Excluir conta</h2>
        <p className="perigo-texto">
          Apaga <strong>permanentemente</strong> sua conta e todos os dados: contas, lançamentos,
          categorias, faturas, recorrências. Não tem volta e não guardamos backup dos seus dados.
        </p>

        {!confirmandoExclusao ? (
          <button className="btn-perigo" onClick={() => setConfirmandoExclusao(true)}>
            Quero excluir minha conta
          </button>
        ) : (
          <form className="form" onSubmit={excluirConta}>
            <div className="linha">
              <label>
                Sua senha
                <input type="password" value={senhaExclusao}
                       onChange={(e) => setSenhaExclusao(e.target.value)} autoComplete="current-password" />
              </label>
              <label>
                Digite <strong>EXCLUIR</strong> pra confirmar
                <input value={textoConfirma} onChange={(e) => setTextoConfirma(e.target.value)}
                       placeholder="EXCLUIR" />
              </label>
            </div>
            <div className="form-pagar-acoes">
              <button type="submit" className="btn-perigo" disabled={excluindo}>
                {excluindo ? 'Excluindo…' : 'Excluir definitivamente'}
              </button>
              <button type="button" className="btn-cancel"
                      onClick={() => { setConfirmandoExclusao(false); setSenhaExclusao(''); setTextoConfirma(''); }}>
                Cancelar
              </button>
            </div>
            {msgExclusao && <p className="msg-inline">{msgExclusao}</p>}
          </form>
        )}
      </section>
    </>
  );
}
