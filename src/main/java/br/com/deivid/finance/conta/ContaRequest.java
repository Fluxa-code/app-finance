package br.com.deivid.finance.conta;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record ContaRequest(
        @NotNull UUID id,
        @NotNull UUID userId,
        @NotBlank String nome,
        @NotNull TipoConta tipo,
        long saldoInicialCents,
        @PositiveOrZero Long limiteCents,
        @Max(31) @Min(1) Integer diaFechamento,
        @Max(31) @Min(1) Integer diaVencimento
) {}
