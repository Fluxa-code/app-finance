import { useState } from 'react';
import Login from './Login';
import Painel from './Painel';
import { getToken } from './api';

// O porteiro: tem crachá guardado? Painel. Não tem? Login.
export default function App() {
  const [logado, setLogado] = useState(() => getToken() != null);

  return logado ? (
    <Painel aoSair={() => setLogado(false)} />
  ) : (
    <Login aoEntrar={() => setLogado(true)} />
  );
}
