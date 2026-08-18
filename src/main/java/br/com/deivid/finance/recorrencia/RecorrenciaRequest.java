package br.com.deivid.finance.recorrencia;

import br.com.deivid.finance.transacao.TipoTransacao;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

public record RecorrenciaRequest(
        @NotNull UUID id,
        @NotNull UUID accountId,
        UUID categoryId,
        @NotNull TipoTransacao tipo,
        @Positive long valorCents,
        @NotBlank String descricao,
        @NotNull Frequencia frequencia,
        @Min(1) @Max(31) int dia,
        @Min(1) @Max(12) Integer mes,       // só ANUAL
        @NotNull LocalDate dataInicio,
        LocalDate dataFim                   // opcional
) {}
