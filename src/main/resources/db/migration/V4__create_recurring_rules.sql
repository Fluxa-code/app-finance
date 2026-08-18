-- ============================================================
-- RECURRING_RULES — a REGRA de um lançamento que se repete.
-- (salário dia 5, aluguel dia 10, Netflix dia 15...)
--
-- A regra NÃO é o lançamento. Ela é a receita: um gerador
-- diário lê as regras e cria as transações reais em
-- `transactions`, cada uma com data e valor próprios.
--
-- Por que assim: assinatura não tem fim; se a gente gerasse
-- 12 meses na frente, no 13º mês pararia. A regra vive, e o
-- gerador cuida do resto.
-- ============================================================
CREATE TABLE recurring_rules (
    id             UUID         PRIMARY KEY,
    user_id        UUID         NOT NULL,
    account_id     UUID         NOT NULL REFERENCES accounts (id),
    category_id    UUID         REFERENCES categories (id),

    tipo           TEXT         NOT NULL CHECK (tipo IN ('ENTRADA', 'SAIDA')),
    valor_cents    BIGINT       NOT NULL CHECK (valor_cents > 0),   -- sempre positivo; sinal é do tipo
    descricao      TEXT         NOT NULL,

    frequencia     TEXT         NOT NULL CHECK (frequencia IN ('MENSAL', 'SEMANAL', 'ANUAL')),
    dia            INT          NOT NULL CHECK (dia BETWEEN 1 AND 31),   -- dia do mês (MENSAL/ANUAL) ou da semana 1-7 (SEMANAL)
    mes            INT          CHECK (mes BETWEEN 1 AND 12),            -- só ANUAL: qual mês

    data_inicio    DATE         NOT NULL,   -- a partir de quando vale
    data_fim       DATE,                    -- NULL = pra sempre (aluguel, Netflix)

    -- até que data já geramos. É o "marcador de página" do gerador:
    -- ele só cria lançamentos com data > proxima_geracao_apos.
    -- Sem isso, rodar o gerador duas vezes duplicaria tudo.
    ultima_geracao DATE,

    ativa          BOOLEAN      NOT NULL DEFAULT true,   -- pausar sem apagar

    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ
);

CREATE INDEX idx_recurring_user  ON recurring_rules (user_id);
CREATE INDEX idx_recurring_ativa ON recurring_rules (ativa) WHERE deleted_at IS NULL;

-- liga cada transação gerada à regra que a criou
-- (pra tela mostrar "🔁 recorrente" e pra excluir a regra poder limpar o futuro)
ALTER TABLE transactions ADD COLUMN recurring_rule_id UUID REFERENCES recurring_rules (id);
CREATE INDEX idx_transactions_recurring ON transactions (recurring_rule_id);
