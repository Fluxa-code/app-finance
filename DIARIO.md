# Diário do app-finance

Anotações rápidas do que travou, o que aprendi e o que rende post no LinkedIn.
2 linhas por dia bastam. Material pra post e pra currículo.

---

## 2026-08-01 — Setup do zero + 5 erros de ambiente
Comecei sem nem ter IDE de Java. Subi Spring Boot 4 + Postgres 18 via Docker Compose.
No caminho, 5 erros encadeados, cada um com causa diferente:
1. Error 1500 — instalador da AMD segurando a fila do Windows Installer.
2. "Cannot run program docker" — IntelliJ com PATH velho (processo herda env de quando nasce).
3. Senha falhou — healthcheck otimista dizendo "healthy" antes do Postgres criar o usuário.
4. Continuou falhando — volume meio-formatado sobrevivendo ao container.
5. Causa real — Postgres nativo do Windows sequestrando a porta 5432.
**Lições:** o último "Caused by" é a verdade; antes de duvidar da senha, veja quem está na porta.
**[📌 LINKEDIN — já postado]**

## 2026-08-01 — Modelagem: por que parcelamento NÃO é 1 linha
Discussão longa: guardar uma compra parcelada como 1 linha quebra o "quanto gastei no mês".
Conclusão: uma compra em N x vira N transações, uma por mês.

## 2026-08-14 — API REST: testar o que TEM que falhar
POST /contas com DTO + Bean Validation. Testei os casos de erro (não só o feliz):
201 (criou), 409 (id duplicado — não sobrescreve conta alheia), 400 (nome vazio, dia 45, tipo inexistente).
**Ideia de post:** API boa se mede pelo que ela RECUSA, não pelo que aceita.
**[📌 LINKEDIN — pendente]**

## 2026-08-15 — Parcelamento: o centavo que não some
R$ 100 em 3x = 3334 + 3333 + 3333 (a 1ª parcela leva o resto da divisão inteira).
Se gravasse 3333 três vezes, a soma daria R$ 99,99 — sumiria um centavo.
Transferência = 2 linhas irmãs (@Transactional: nascem juntas ou nenhuma).
Saldo nunca é campo guardado — é sempre SUM(transações). Livro-razão.
**Ideia de post:** por que uma compra em 10x vira 10 linhas, e como o centavo não some.
**[📌 LINKEDIN — pendente]**

## 2026-08-17 — Fatura + primeira tela + TEMPO REAL 🏆
Fatura: compra no cartão cai na fatura certa pela regra do fechamento (dia<=fechamento →
mês atual; senão → mês seguinte), find-or-create, fecha num mês e vence no seguinte.
Front React/Vite: cards de saldo, formulários (gasto/transferência/parcelamento), extrato clicável.
TEMPO REAL via SSE: cada tela mantém conexão aberta (GET /eventos), o service publica
"mudanca" a cada lançamento, todas as telas recarregam sozinhas. O TESTE MÁGICO passou:
duas janelas lado a lado, lancei numa, a outra atualizou sem F5.
Bug de CSS aprendido: `.form button` (elemento genérico) venceu `.aba` por especificidade
e pintou as abas de azul — seletor amplo pega quem não devia.
**[📌 LINKEDIN — POSTADO com GIF das duas telas. O post campeão.]**
