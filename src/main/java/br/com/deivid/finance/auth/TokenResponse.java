package br.com.deivid.finance.auth;

import java.time.Instant;

// o que o app recebe ao logar: o crachá (token), quando ele vence, e o nome pra UI
public record TokenResponse(
        String token,
        Instant expiraEm,
        String nome
) {}
