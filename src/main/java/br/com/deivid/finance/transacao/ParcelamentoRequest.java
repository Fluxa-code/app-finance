package br.com.deivid.finance.transacao;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public record ParcelamentoRequest(
        @NotNull UUID id,               // id do cabeçalho (gerado no cliente)
        @NotNull UUID userId,
        @NotNull UUID accountId,
        UUID categoryId,                // opcional
        String descricao,
        @Positive long valorTotalCents, // valor TOTAL da compra (positivo)
        @Min(2) int parcelaTotal,       // 2x ou mais — 1x é compra à vista (usa criarSimples)
        @NotNull LocalDate dataPrimeira // data da 1ª parcela
) {}
