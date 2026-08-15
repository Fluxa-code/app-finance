-- ============================================================
-- PARCELAMENTOS — o "cabeçalho" de uma compra parcelada.
-- Guarda a compra inteira (R$ 1.200 em 10x). As 10 parcelas
-- viram linhas em transactions, todas apontando pra cá.
-- ============================================================
CREATE TABLE parcelamentos (
    id                 UUID        PRIMARY KEY,
    user_id            UUID        NOT NULL,
    account_id         UUID        NOT NULL REFERENCES accounts (id),
    descricao          TEXT,
    valor_total_cents  BIGINT      NOT NULL,
    parcela_total      INT         NOT NULL CHECK (parcela_total >= 1),
    data_primeira      DATE        NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at         TIMESTAMPTZ
);

-- ============================================================
-- INVOICES — a fatura do cartão. Tem estado que muda no tempo:
-- ABERTA -> FECHADA -> PAGA. O card_id aponta pra uma account
-- do tipo CARTAO_CREDITO (cartão é conta, lembra).
-- O VALOR da fatura NÃO é guardado aqui — é a soma das
-- transactions daquela fatura (princípio do livro-razão).
-- ============================================================
CREATE TABLE invoices (
    id               UUID        PRIMARY KEY,
    user_id          UUID        NOT NULL,
    card_id          UUID        NOT NULL REFERENCES accounts (id),
    ano              INT         NOT NULL,
    mes              INT         NOT NULL CHECK (mes BETWEEN 1 AND 12),
    data_fechamento  DATE        NOT NULL,
    data_vencimento  DATE        NOT NULL,
    status           TEXT        NOT NULL DEFAULT 'ABERTA'
                                 CHECK (status IN ('ABERTA', 'FECHADA', 'PAGA')),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ,

    -- um cartão não pode ter duas faturas do mesmo mês
    UNIQUE (card_id, ano, mes)
);

-- ============================================================
-- TRANSACTIONS — a peça central. Todo movimento de dinheiro
-- é uma linha aqui.
-- ============================================================
CREATE TABLE transactions (
    id               UUID        PRIMARY KEY,
    user_id          UUID        NOT NULL,
    account_id       UUID        NOT NULL REFERENCES accounts (id),
    category_id      UUID        REFERENCES categories (id),   -- nullable: transferência não tem categoria

    tipo             TEXT        NOT NULL
                                 CHECK (tipo IN ('ENTRADA', 'SAIDA', 'TRANSFERENCIA')),
    valor_cents      BIGINT      NOT NULL,   -- sinalizado: entra positivo, sai negativo
    descricao        TEXT,
    data             DATE        NOT NULL,

    -- liga as 2 linhas de uma transferência (mesmo UUID nas duas)
    transfer_id      UUID,

    -- liga as N parcelas de uma compra parcelada
    parcelamento_id  UUID        REFERENCES parcelamentos (id),
    parcela_num      INT,        -- "3" de 3/10
    parcela_total    INT,        -- "10" de 3/10

    -- em qual fatura essa compra de cartão caiu (nullable: débito não tem fatura)
    invoice_id       UUID        REFERENCES invoices (id),

    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ
);

-- ============================================================
-- ÍNDICES — o app vai filtrar transações por essas colunas o
-- tempo todo (extrato, saldo, agrupar irmãs). Sem índice, o
-- Postgres varre a tabela inteira a cada consulta; com índice,
-- ele vai direto. Isso é o que segura performance quando a
-- base cresce — importa pra escalar.
-- ============================================================
CREATE INDEX idx_transactions_account   ON transactions (account_id);
CREATE INDEX idx_transactions_user       ON transactions (user_id);
CREATE INDEX idx_transactions_transfer   ON transactions (transfer_id);
CREATE INDEX idx_transactions_parcela    ON transactions (parcelamento_id);
CREATE INDEX idx_transactions_invoice    ON transactions (invoice_id);
