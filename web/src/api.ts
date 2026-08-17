// ============================================================
// Camada de API do front — o espelho do "service" do backend.
// TODA chamada passa por aqui: um lugar só anexa o token,
// um lugar só trata sessão expirada.
// ============================================================

// vem do .env.development (npm run dev) ou .env.production (npm run build)
const API = import.meta.env.VITE_API_URL;
export const API_URL = API; // pro EventSource (SSE)

// ---- sessão (token guardado no navegador) ----

export function getToken(): string | null {
  return localStorage.getItem('token');
}

export function getNome(): string | null {
  return localStorage.getItem('nome');
}

export function salvarSessao(token: string, nome: string) {
  localStorage.setItem('token', token);
  localStorage.setItem('nome', nome);
}

export function limparSessao() {
  localStorage.removeItem('token');
  localStorage.removeItem('nome');
}

// erro especial: "seu crachá venceu" — quem chamar decide voltar pro login
export class NaoAutenticado extends Error {}

// ---- o coração: fetch com token + tratamento de 401 ----

async function request(caminho: string, options: RequestInit = {}): Promise<Response> {
  const headers: Record<string, string> = {
    ...((options.headers as Record<string, string>) ?? {}),
  };

  // anexa o crachá em tudo — MENOS no /auth (login não precisa de token,
  // e um token velho/vencido no header faria o próprio login falhar)
  const token = getToken();
  if (token && !caminho.startsWith('/auth')) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  if (options.body) {
    headers['Content-Type'] = 'application/json';
  }

  const resp = await fetch(`${API}${caminho}`, { ...options, headers });

  // 401 fora do /auth = sessão expirou → limpa e avisa quem chamou
  if (resp.status === 401 && !caminho.startsWith('/auth')) {
    limparSessao();
    throw new NaoAutenticado();
  }

  return resp;
}

async function mensagemDeErro(resp: Response): Promise<string> {
  const err = await resp.json().catch(() => null);
  return err?.mensagem ?? 'Erro inesperado.';
}

export const api = {
  async get<T>(caminho: string): Promise<T> {
    const resp = await request(caminho);
    if (!resp.ok) throw new Error(await mensagemDeErro(resp));
    return resp.json();
  },

  async post<T>(caminho: string, body: unknown): Promise<T> {
    const resp = await request(caminho, { method: 'POST', body: JSON.stringify(body) });
    if (!resp.ok) throw new Error(await mensagemDeErro(resp));
    return resp.json();
  },
};
