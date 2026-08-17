package br.com.deivid.finance.transacao;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

// SEM userId — a identidade vem do token
public record TransacaoRequest(
        @NotNull UUID id,            // gerado no cliente (offline-first)
        @NotNull UUID accountId,
        UUID categoryId,             // opcional
        @NotNull TipoTransacao tipo,
        @Positive long valorCents,   // sempre POSITIVO — o service aplica o sinal
        String descricao,
        @NotNull LocalDate data
) {}
