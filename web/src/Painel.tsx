import { useEffect, useState, type FormEvent } from 'react';
import { api, API_URL, getNome, limparSessao, NaoAutenticado } from './api';
import { useTema } from './useTema';
import Dashboard from './Dashboard';
import Faturas from './Faturas';
import ModalLancamento from './ModalLancamento';
import {
  CORES, descreverRecorrencia, formatBRL, formatData, TIPO_ICONE, TIPO_LABEL,
  type Categoria, type Conta, type Fatura, type GastoCategoria, type Recorrencia, type Saldo, type Transacao,
} from './tipos';
import './App.css';

type Vista =
  | { tela: 'INICIO' }
  | { tela: 'CONTA'; id: string }
  | { tela: 'CATEGORIAS' }
  | { tela: 'RECORRENCIAS' };

export default function Painel({ aoSair }: { aoSair: () => void }) {
  const { tema, alternar } = useTema();

  const [contas, setContas] = useState<Conta[]>([]);
  const [saldos, setSaldos] = useState<Saldo[]>([]);
  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [recentes, setRecentes] = useState<Transacao[]>([]);
  const [transacoesMes, setTransacoesMes] = useState<Transacao[]>([]);
  const [relatorio, setRelatorio] = useState<GastoCategoria[]>([]);
  const [extrato, setExtrato] = useState<Transacao[]>([]);
  const [recorrencias, setRecorrencias] = useState<Recorrencia[]>([]);
  const [faturas, setFaturas] = useState<Fatura[]>([]);

  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [tick, setTick] = useState(0);

  const [vista, setVista] = useState<Vista>({ tela: 'INICIO' });
  const [modalAberto, setModalAberto] = useState(false);
  const [menuMobile, setMenuMobile] = useState(false);

  const hoje = new Date();
  const [ano, setAno] = useState(hoje.getFullYear());
  const [mes, setMes] = useState(hoje.getMonth() + 1);

  // edição de lançamento
  const [editando, setEditando] = useState<Transacao | null>(null);
  const [edValor, setEdValor] = useState('');
  const [edDescricao, setEdDescricao] = useState('');
  const [edData, setEdData] = useState('');
  const [edMsg, setEdMsg] = useState<string | null>(null);

  // nova conta
  const [novaContaAberta, setNovaContaAberta] = useState(false);
  const [ncNome, setNcNome] = useState('');
  const [ncTipo, setNcTipo] = useState('CORRENTE');
  const [ncSaldo, setNcSaldo] = useState('');
  const [ncLimite, setNcLimite] = useState('');
  const [ncFechamento, setNcFechamento] = useState('');
  const [ncVencimento, setNcVencimento] = useState('');
  const [ncMsg, setNcMsg] = useState<string | null>(null);

  // nova categoria
  const [novaCatNome, setNovaCatNome] = useState('');
  const [novaCatTipo, setNovaCatTipo] = useState('DESPESA');
  const [novaCatCor, setNovaCatCor] = useState(CORES[0]);
  const [catMsg, setCatMsg] = useState<string | null>(null);

  function tratarFalha(err: unknown) {
    if (err instanceof NaoAutenticado) {
      aoSair();
      return true;
    }
    return false;
  }

  const recarregar = () => setTick((t) => t + 1);

  // dados gerais
  useEffect(() => {
    (async () => {
      try {
        const c = await api.get<Conta[]>('/contas');
        setContas(c);
        setSaldos(await Promise.all(c.map((x) => api.get<Saldo>(`/contas/${x.id}/saldo`))));
        setCategorias(await api.get<Categoria[]>('/categorias'));
        setRecentes(await api.get<Transacao[]>('/transacoes'));
        setRecorrencias(await api.get<Recorrencia[]>('/recorrencias'));
        setFaturas(await api.get<Fatura[]>('/faturas'));
        setErro(null);
      } catch (err) {
        if (!tratarFalha(err)) setErro('Não consegui falar com a API. O backend está rodando?');
      } finally {
        setCarregando(false);
      }
    })();
  }, [tick]);

  // dados do mês (KPIs + relatório)
  useEffect(() => {
    (async () => {
      try {
        setTransacoesMes(await api.get<Transacao[]>(`/transacoes/mes?ano=${ano}&mes=${mes}`));
        setRelatorio(await api.get<GastoCategoria[]>(`/categorias/relatorio?ano=${ano}&mes=${mes}`));
      } catch (err) {
        if (!tratarFalha(err)) { setTransacoesMes([]); setRelatorio([]); }
      }
    })();
  }, [ano, mes, tick]);

  // extrato da conta aberta
  useEffect(() => {
    if (vista.tela !== 'CONTA') { setExtrato([]); return; }
    api.get<Transacao[]>(`/transacoes/conta/${vista.id}`)
      .then(setExtrato)
      .catch((err) => { if (!tratarFalha(err)) setExtrato([]); });
  }, [vista, tick]);

  // tempo real
  useEffect(() => {
    const es = new EventSource(`${API_URL}/eventos`);
    es.addEventListener('mudanca', () => setTick((t) => t + 1));
    return () => es.close();
  }, []);

  // ---------- ações ----------
  function parseReais(txt: string): number | null {
    if (!txt.trim()) return null;
    const n = Number(txt.replace(',', '.'));
    return Number.isFinite(n) ? Math.round(n * 100) : null;
  }

  async function criarConta(e: FormEvent) {
    e.preventDefault();
    setNcMsg(null);
    if (!ncNome.trim()) return setNcMsg('Dá um nome pra conta.');

    const cartao = ncTipo === 'CARTAO_CREDITO';
    if (cartao && !ncFechamento) return setNcMsg('Cartão precisa do dia de fechamento.');

    const body: Record<string, unknown> = {
      id: crypto.randomUUID(),
      nome: ncNome.trim(),
      tipo: ncTipo,
      saldoInicialCents: parseReais(ncSaldo) ?? 0,
    };
    if (cartao) {
      body.limiteCents = parseReais(ncLimite) ?? undefined;
      body.diaFechamento = Number(ncFechamento);
      if (ncVencimento) body.diaVencimento = Number(ncVencimento);
    }
    try {
      await api.post('/contas', body);
      setNcNome(''); setNcSaldo(''); setNcLimite(''); setNcFechamento(''); setNcVencimento('');
      setNovaContaAberta(false);
      recarregar();
    } catch (err) {
      if (!tratarFalha(err)) setNcMsg(err instanceof Error ? err.message : 'Erro.');
    }
  }

  async function criarCategoria(e: FormEvent) {
    e.preventDefault();
    setCatMsg(null);
    if (!novaCatNome.trim()) return setCatMsg('Dá um nome pra categoria.');
    try {
      await api.post('/categorias', {
        id: crypto.randomUUID(), nome: novaCatNome.trim(),
        tipo: novaCatTipo, cor: novaCatCor,
      });
      setNovaCatNome('');
      recarregar();
    } catch (err) {
      if (!tratarFalha(err)) setCatMsg(err instanceof Error ? err.message : 'Erro.');
    }
  }

  async function alternarRecorrencia(r: Recorrencia) {
    try { await api.patch(`/recorrencias/${r.id}/alternar`); recarregar(); }
    catch (err) { if (!tratarFalha(err)) alert('Erro ao alterar.'); }
  }

  async function excluirRecorrencia(r: Recorrencia) {
    if (!confirm(`Excluir "${r.descricao}"? Os lançamentos FUTUROS somem; os passados ficam.`)) return;
    try { await api.del(`/recorrencias/${r.id}`); recarregar(); }
    catch (err) { if (!tratarFalha(err)) alert('Erro ao excluir.'); }
  }

  async function excluirCategoria(c: Categoria) {
    if (!confirm(`Excluir "${c.nome}"? Os lançamentos dela viram "Sem categoria".`)) return;
    try { await api.del(`/categorias/${c.id}`); recarregar(); }
    catch (err) { if (!tratarFalha(err)) alert('Erro ao excluir.'); }
  }

  function abrirEdicao(t: Transacao) {
    setEditando(t);
    setEdValor((Math.abs(t.valorCents) / 100).toFixed(2).replace('.', ','));
    setEdDescricao(t.descricao ?? '');
    setEdData(t.data);
    setEdMsg(null);
  }

  async function salvarEdicao(e: FormEvent) {
    e.preventDefault();
    if (!editando) return;
    const valorReais = Number(edValor.replace(',', '.'));
    if (!valorReais || valorReais <= 0) return setEdMsg('Valor maior que zero.');
    try {
      await api.put(`/transacoes/${editando.id}`, {
        accountId: editando.accountId,
        categoryId: editando.categoryId,
        valorCents: Math.round(valorReais * 100),
        descricao: edDescricao,
        data: edData,
      });
      setEditando(null);
      recarregar();
    } catch (err) {
      if (!tratarFalha(err)) setEdMsg(err instanceof Error ? err.message : 'Erro.');
    }
  }

  async function excluirTransacao(t: Transacao) {
    const aviso = t.transferId
      ? 'Excluir esta transferência? Os dois lados serão removidos.'
      : t.parcelamentoId
        ? `Excluir esta compra parcelada? TODAS as ${t.parcelaTotal} parcelas serão removidas.`
        : `Excluir "${t.descricao || 'este lançamento'}"?`;
    if (!confirm(aviso)) return;
    try { await api.del(`/transacoes/${t.id}`); recarregar(); }
    catch (err) { if (!tratarFalha(err)) alert('Erro ao excluir.'); }
  }

  if (carregando) return <p className="status">Carregando…</p>;
  if (erro) return <p className="status erro">{erro}</p>;

  const saldoDe = (id: string) => saldos.find((s) => s.contaId === id)?.saldoCents ?? 0;
  const contaAberta = vista.tela === 'CONTA' ? contas.find((c) => c.id === vista.id) : null;

  return (
    <div className={menuMobile ? 'shell menu-aberto' : 'shell'}>
      {/* ============ SIDEBAR ============ */}
      <aside className="sidebar">
        <div className="marca">
          <div className="logo">K</div>
          <div>
            <h1>Kofre</h1>
            <span className="marca-sub">Fluxa Labs</span>
          </div>
        </div>

        <button className="btn-novo" onClick={() => setModalAberto(true)}>
          <span>+</span> Novo lançamento
        </button>

        <nav className="nav">
          <button
            className={vista.tela === 'INICIO' ? 'nav-item ativo' : 'nav-item'}
            onClick={() => { setVista({ tela: 'INICIO' }); setMenuMobile(false); }}
          >
            <span className="nav-icone">◧</span> Início
          </button>
          <button
            className={vista.tela === 'RECORRENCIAS' ? 'nav-item ativo' : 'nav-item'}
            onClick={() => { setVista({ tela: 'RECORRENCIAS' }); setMenuMobile(false); }}
          >
            <span className="nav-icone">🔁</span> Recorrentes
            {recorrencias.filter((r) => r.ativa).length > 0 && (
              <span className="nav-badge">{recorrencias.filter((r) => r.ativa).length}</span>
            )}
          </button>
          <button
            className={vista.tela === 'CATEGORIAS' ? 'nav-item ativo' : 'nav-item'}
            onClick={() => { setVista({ tela: 'CATEGORIAS' }); setMenuMobile(false); }}
          >
            <span className="nav-icone">◈</span> Categorias
          </button>
        </nav>

        <div className="sec-titulo">
          Contas
          <button className="btn-mini" onClick={() => setNovaContaAberta((v) => !v)} title="Nova conta">+</button>
        </div>

        <ul className="lista-contas">
          {contas.map((c) => (
            <li key={c.id}>
              <button
                className={vista.tela === 'CONTA' && vista.id === c.id ? 'conta-item ativo' : 'conta-item'}
                onClick={() => { setVista({ tela: 'CONTA', id: c.id }); setMenuMobile(false); }}
              >
                <span className="conta-icone">{TIPO_ICONE[c.tipo] ?? '💰'}</span>
                <span className="conta-nome">{c.nome}</span>
                <span className={saldoDe(c.id) < 0 ? 'conta-saldo negativo' : 'conta-saldo'}>
                  {formatBRL(saldoDe(c.id))}
                </span>
              </button>
            </li>
          ))}
          {contas.length === 0 && <li className="vazio-mini">Nenhuma conta ainda</li>}
        </ul>

        <div className="sidebar-rodape">
          <span className="usuario-nome">{getNome()}</span>
          <button className="btn-icone" onClick={alternar} title="Tema">
            {tema === 'dark' ? '☀' : '☾'}
          </button>
          <button className="btn-icone" onClick={() => { limparSessao(); aoSair(); }} title="Sair">⏻</button>
        </div>
      </aside>

      {/* fundo escuro que fecha o menu no celular */}
      <div className="backdrop" onClick={() => setMenuMobile(false)} />

      {/* ============ CONTEÚDO ============ */}
      <main className="conteudo">
        <button className="btn-menu" onClick={() => setMenuMobile(true)} title="Menu">☰</button>

        {vista.tela === 'INICIO' && (
          <Dashboard
            saldos={saldos}
            transacoesMes={transacoesMes}
            recentes={recentes}
            relatorio={relatorio}
            categorias={categorias}
            faturas={faturas}
            ano={ano}
            mes={mes}
            onTrocarMes={(a, m) => { setAno(a); setMes(m); }}
            onVerConta={(id) => setVista({ tela: 'CONTA', id })}
          />
        )}

        {vista.tela === 'CONTA' && contaAberta && (
          <>
            <header className="cabecalho">
              <div>
                <span className="cab-tipo">{TIPO_LABEL[contaAberta.tipo]}</span>
                <h2>{contaAberta.nome}</h2>
              </div>
              <strong className={saldoDe(contaAberta.id) < 0 ? 'cab-saldo negativo' : 'cab-saldo'}>
                {formatBRL(saldoDe(contaAberta.id))}
              </strong>
            </header>

            {contaAberta.tipo === 'CARTAO_CREDITO' && (
              <Faturas
                cardId={contaAberta.id}
                contas={contas}
                tick={tick}
                aoMudar={recarregar}
                aoDeslogar={aoSair}
              />
            )}

            <section className="bloco extrato">
              <h2>{contaAberta.tipo === 'CARTAO_CREDITO' ? 'Todos os lançamentos' : 'Extrato'}</h2>
              {extrato.length === 0 ? (
                <p className="vazio">Nenhum lançamento nesta conta.</p>
              ) : (
                <ul>
                  {extrato.map((t) =>
                    editando?.id === t.id ? (
                      <li key={t.id} className="editando">
                        <form className="form-edicao" onSubmit={salvarEdicao}>
                          <input type="date" value={edData} onChange={(e) => setEdData(e.target.value)} />
                          <input value={edDescricao} onChange={(e) => setEdDescricao(e.target.value)} placeholder="Descrição" />
                          <input inputMode="decimal" className="input-valor" value={edValor}
                                 onChange={(e) => setEdValor(e.target.value)} placeholder="0,00" />
                          <button type="submit" className="btn-ok">Salvar</button>
                          <button type="button" className="btn-cancel" onClick={() => setEditando(null)}>Cancelar</button>
                          {edMsg && <span className="msg-inline">{edMsg}</span>}
                        </form>
                      </li>
                    ) : (
                      <li key={t.id}>
                        <span className="ext-data">{formatData(t.data)}</span>
                        <span className="ext-desc">
                          {t.descricao || '(sem descrição)'}
                          {t.parcelaNum != null && <em className="badge">{t.parcelaNum}/{t.parcelaTotal}</em>}
                          {t.tipo === 'TRANSFERENCIA' && <em className="badge transf">transferência</em>}
                          {t.recurringRuleId && <em className="badge" title="Gerado por recorrência">🔁</em>}
                        </span>
                        <span className={t.valorCents < 0 ? 'ext-valor negativo' : 'ext-valor positivo'}>
                          {formatBRL(t.valorCents)}
                        </span>
                        <span className="acoes">
                          {!t.transferId && !t.parcelamentoId && (
                            <button className="btn-icone" title="Editar" onClick={() => abrirEdicao(t)}>✏️</button>
                          )}
                          <button className="btn-icone" title="Excluir" onClick={() => excluirTransacao(t)}>🗑️</button>
                        </span>
                      </li>
                    ),
                  )}
                </ul>
              )}
            </section>
          </>
        )}

        {vista.tela === 'RECORRENCIAS' && (
          <>
            <header className="cabecalho">
              <div>
                <span className="cab-tipo">Lançamentos que se repetem</span>
                <h2>Recorrentes</h2>
              </div>
              <button className="btn-novo compacto" onClick={() => setModalAberto(true)}>
                + Nova recorrência
              </button>
            </header>

            <section className="bloco">
              {recorrencias.length === 0 ? (
                <p className="vazio">
                  Nenhuma recorrência ainda. Salário, aluguel, assinatura — cadastra uma vez,
                  o Kofre lança sozinho todo mês.
                </p>
              ) : (
                <ul className="rec-lista">
                  {recorrencias.map((r) => {
                    const cat = categorias.find((c) => c.id === r.categoryId);
                    const conta = contas.find((c) => c.id === r.accountId);
                    return (
                      <li key={r.id} className={r.ativa ? '' : 'pausada'}>
                        <i className="ponto" style={{ background: cat?.cor ?? 'var(--text-faint)' }} />
                        <div className="rec-info">
                          <span className="rec-titulo">
                            {r.descricao}
                            {!r.ativa && <em className="badge">pausada</em>}
                          </span>
                          <span className="rec-sub">
                            {descreverRecorrencia(r)} · {conta?.nome ?? '—'}
                            {r.dataFim && ` · até ${formatData(r.dataFim)}`}
                          </span>
                        </div>
                        <span className={r.tipo === 'SAIDA' ? 'ext-valor negativo' : 'ext-valor positivo'}>
                          {r.tipo === 'SAIDA' ? '-' : '+'}{formatBRL(r.valorCents)}
                        </span>
                        <span className="acoes sempre">
                          <button className="btn-icone" title={r.ativa ? 'Pausar' : 'Retomar'}
                                  onClick={() => alternarRecorrencia(r)}>
                            {r.ativa ? '⏸' : '▶'}
                          </button>
                          <button className="btn-icone" title="Excluir" onClick={() => excluirRecorrencia(r)}>🗑️</button>
                        </span>
                      </li>
                    );
                  })}
                </ul>
              )}
            </section>
          </>
        )}

        {vista.tela === 'CATEGORIAS' && (
          <>
            <header className="cabecalho">
              <div><h2>Categorias</h2></div>
            </header>
            <section className="bloco">
              <form className="cat-form" onSubmit={criarCategoria}>
                <input value={novaCatNome} onChange={(e) => setNovaCatNome(e.target.value)}
                       placeholder="Alimentação, Transporte…" />
                <select value={novaCatTipo} onChange={(e) => setNovaCatTipo(e.target.value)}>
                  <option value="DESPESA">Despesa</option>
                  <option value="RECEITA">Receita</option>
                </select>
                <div className="cores">
                  {CORES.map((cor) => (
                    <button key={cor} type="button" style={{ background: cor }}
                            className={novaCatCor === cor ? 'cor ativa' : 'cor'}
                            onClick={() => setNovaCatCor(cor)} title={cor} />
                  ))}
                </div>
                <button type="submit" className="btn-ok">Criar</button>
              </form>
              {catMsg && <p className="msg">{catMsg}</p>}

              <ul className="cat-lista">
                {categorias.map((c) => (
                  <li key={c.id}>
                    <i className="ponto" style={{ background: c.cor ?? 'var(--text-faint)' }} />
                    {c.nome}
                    <em className="badge">{c.tipo === 'RECEITA' ? 'receita' : 'despesa'}</em>
                    <button className="btn-icone" onClick={() => excluirCategoria(c)} title="Excluir">🗑️</button>
                  </li>
                ))}
                {categorias.length === 0 && <li className="vazio">Nenhuma categoria ainda.</li>}
              </ul>
            </section>
          </>
        )}

        {/* nova conta (abre pelo + da sidebar, em qualquer tela) */}
        {novaContaAberta && (
          <form className="bloco form" onSubmit={criarConta}>
            <h2>Nova conta</h2>
            <div className="linha">
              <label>Nome
                <input value={ncNome} onChange={(e) => setNcNome(e.target.value)} placeholder="Nubank, Carteira…" />
              </label>
              <label>Tipo
                <select value={ncTipo} onChange={(e) => setNcTipo(e.target.value)}>
                  <option value="CORRENTE">Conta corrente</option>
                  <option value="POUPANCA">Poupança</option>
                  <option value="CARTEIRA">Carteira</option>
                  <option value="CARTAO_CREDITO">Cartão de crédito</option>
                </select>
              </label>
            </div>
            <div className="linha">
              <label>Saldo inicial (R$)
                <input inputMode="decimal" placeholder="0,00" value={ncSaldo} onChange={(e) => setNcSaldo(e.target.value)} />
              </label>
              {ncTipo === 'CARTAO_CREDITO' && (
                <label>Limite (R$)
                  <input inputMode="decimal" placeholder="0,00" value={ncLimite} onChange={(e) => setNcLimite(e.target.value)} />
                </label>
              )}
            </div>
            {ncTipo === 'CARTAO_CREDITO' && (
              <div className="linha">
                <label>Dia de fechamento
                  <input type="number" min={1} max={31} value={ncFechamento} onChange={(e) => setNcFechamento(e.target.value)} />
                </label>
                <label>Dia de vencimento
                  <input type="number" min={1} max={31} value={ncVencimento} onChange={(e) => setNcVencimento(e.target.value)} />
                </label>
              </div>
            )}
            <button type="submit">Criar conta</button>
            {ncMsg && <p className="msg">{ncMsg}</p>}
          </form>
        )}
      </main>

      {modalAberto && (
        <ModalLancamento
          contas={contas}
          categorias={categorias}
          contaPadrao={vista.tela === 'CONTA' ? vista.id : undefined}
          aoFechar={() => setModalAberto(false)}
          aoLancar={recarregar}
          aoDeslogar={aoSair}
        />
      )}
    </div>
  );
}
