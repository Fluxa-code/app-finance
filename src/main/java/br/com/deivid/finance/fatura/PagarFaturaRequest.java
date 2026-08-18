package br.com.deivid.finance.fatura;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Pagar fatura = transferir da conta corrente pro cartão.
 *
 * O valor vem no request (e não é lido da fatura) porque pagamento
 * parcial existe: a pessoa pode pagar R$ 500 de uma fatura de R$ 800.
 */
public record PagarFaturaRequest(
        @NotNull UUID contaOrigemId,   // de onde sai o dinheiro
        @Positive long valorCents,
        @NotNull LocalDate data
) {}
