package br.com.deivid.finance.fatura;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma fatura como a tela vê.
 *
 * `totalCents` NÃO é coluna: é a soma das transações da fatura, calculada
 * na hora (livro-razão). Positivo = quanto você deve.
 * `status` é DERIVADO da data — ver FaturaService.statusDe().
 */
public record FaturaResponse(
        UUID id,
        UUID cardId,
        String cartaoNome,
        int ano,
        int mes,
        LocalDate dataFechamento,
        LocalDate dataVencimento,
        StatusFatura status,
        long totalCents,
        long quantidade
) {}
