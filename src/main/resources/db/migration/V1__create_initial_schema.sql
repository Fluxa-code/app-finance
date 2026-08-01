CREATE TABLE accounts
(
        id                  UUID PRIMARY KEY,
        user_id             UUID       NOT NULL,
        nome                TEXT       NOT NULL,
        tipo                TEXT       NOT NULL CHECK ( tipo IN ('CARTAO_CREDITO', 'POUPANCA', 'CORRENTE', 'CARTEIRA')),
        saldo_inicial_cents BIGINT     NOT NULL DEFAULT 0,
        limite_cents        BIGINT,
        dia_fechamento      int CHECK ( dia_fechamento between 1 and 31),
        dia_vencimento      int CHECK ( dia_vencimento between 1 and 31),
        created_at          timestamptz not null default now(),
        updated_at          timestamptz not null default now(),
        deleted_at          timestamptz
);

CREATE TABLE categories (
                            id          UUID         PRIMARY KEY,
                            user_id     UUID         NOT NULL,
                            nome        TEXT         NOT NULL,
                            tipo        TEXT         NOT NULL CHECK (tipo IN ('RECEITA', 'DESPESA')),
                            cor         TEXT,
                            created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                            updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                            deleted_at  TIMESTAMPTZ
);





