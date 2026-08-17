-- ============================================================
-- USERS — quem usa o app. A partir daqui, todo user_id das
-- outras tabelas passa a apontar pra alguém de verdade.
--
-- senha_hash: NUNCA guardamos a senha. Guardamos o hash BCrypt
-- dela — um resumo irreversível. Nem o dono do banco consegue
-- descobrir a senha original.
-- ============================================================
CREATE TABLE users (
    id          UUID         PRIMARY KEY,
    email       TEXT         NOT NULL UNIQUE,
    senha_hash  TEXT         NOT NULL,
    nome        TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);
