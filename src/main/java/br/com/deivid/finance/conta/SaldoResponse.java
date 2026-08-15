package br.com.deivid.finance.conta;

import java.util.UUID;

// DTO de RESPOSTA: controla o que sai da API, separado da entidade.
public record SaldoResponse(
        UUID contaId,
        String nome,
        long saldoCents
) {}
