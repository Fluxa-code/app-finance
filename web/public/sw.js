/* ============================================================
   Service Worker do Kofre.
   Um script que roda FORA da página, no navegador, e intercepta
   as requisições. É ele que permite "instalar" o app e fazer
   ele abrir mesmo com internet ruim.

   ⚠️ REGRA DE OURO AQUI: NUNCA cachear chamada de API.
   Dado financeiro em cache = usuário vendo saldo velho e
   achando que é o atual. O cache serve só pra CASCA do app
   (html, js, css) — os dados vêm sempre da rede.
   ============================================================ */

const CACHE = 'kofre-v1';
const CASCA = ['/', '/index.html', '/manifest.webmanifest'];

self.addEventListener('install', (evento) => {
  evento.waitUntil(
    caches.open(CACHE).then((c) => c.addAll(CASCA)).then(() => self.skipWaiting()),
  );
});

// ao ativar, apaga caches de versões antigas
self.addEventListener('activate', (evento) => {
  evento.waitUntil(
    caches.keys()
      .then((chaves) => Promise.all(chaves.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener('fetch', (evento) => {
  const req = evento.request;
  const url = new URL(req.url);

  // só GET, só mesmo domínio: API e POST passam direto, sem tocar no cache
  if (req.method !== 'GET' || url.origin !== self.location.origin) return;

  // navegação (abrir o app): tenta a rede primeiro, cai no cache se offline
  if (req.mode === 'navigate') {
    evento.respondWith(fetch(req).catch(() => caches.match('/index.html')));
    return;
  }

  // arquivos com hash no nome (/assets/index-A1b2C3.js) são imutáveis:
  // se está no cache, pode servir direto — o nome muda quando o conteúdo muda
  if (url.pathname.startsWith('/assets/')) {
    evento.respondWith(
      caches.match(req).then((emCache) =>
        emCache ||
        fetch(req).then((resp) => {
          const copia = resp.clone();
          caches.open(CACHE).then((c) => c.put(req, copia));
          return resp;
        }),
      ),
    );
  }
});
