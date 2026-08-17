package br.com.deivid.finance.conta;

import jakarta.validation.constraints.*;

import java.util.UUID;

// repara: SEM userId. A identidade vem do token, nunca do corpo da requisição.
public record ContaRequest(
        @NotNull UUID id,
        @NotBlank String nome,
        @NotNull TipoConta tipo,
        long saldoInicialCents,
        @PositiveOrZero Long limiteCents,
        @Max(31) @Min(1) Integer diaFechamento,
        @Max(31) @Min(1) Integer diaVencimento
) {}
