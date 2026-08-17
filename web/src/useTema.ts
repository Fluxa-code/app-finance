import { useEffect, useState } from 'react';

type Tema = 'light' | 'dark';

/**
 * Tema claro/escuro.
 *
 * Ordem de decisão:
 *  1. o que o usuário escolheu antes (localStorage)
 *  2. senão, a preferência do SISTEMA dele (prefers-color-scheme)
 *
 * Respeitar o sistema por padrão é o comportamento educado: quem
 * deixa o celular no escuro à noite espera que o app acompanhe.
 */
export function useTema() {
  const [tema, setTema] = useState<Tema>(() => {
    const salvo = localStorage.getItem('tema') as Tema | null;
    if (salvo) return salvo;
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', tema);
    localStorage.setItem('tema', tema);
  }, [tema]);

  const alternar = () => setTema((t) => (t === 'dark' ? 'light' : 'dark'));

  return { tema, alternar };
}
