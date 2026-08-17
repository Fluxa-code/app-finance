package br.com.deivid.finance.transacao;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

// SEM userId — a identidade vem do token
public record TransferenciaRequest(
        @NotNull UUID transferId,      // id do PAR (gerado no cliente) — liga as duas linhas
        @NotNull UUID origemId,        // linha da SAÍDA nasce com este id
        @NotNull UUID destinoId,       // linha da ENTRADA nasce com este id
        @NotNull UUID contaOrigemId,   // de qual conta sai
        @NotNull UUID contaDestinoId,  // pra qual conta entra
        @Positive long valorCents,     // valor positivo; o service põe o sinal em cada lado
        String descricao,
        @NotNull LocalDate data
) {}
