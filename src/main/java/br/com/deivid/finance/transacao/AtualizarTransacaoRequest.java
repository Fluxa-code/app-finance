package br.com.deivid.finance.transacao;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

/**
 * O que pode ser EDITADO num lançamento.
 *
 * Repara no que NÃO está aqui: id, userId, tipo, transferId,
 * parcelamentoId. Esses são identidade/estrutura — mudar eles
 * transformaria o lançamento em outra coisa. Editar é corrigir
 * um erro de digitação, não trocar a natureza do registro.
 */
public record AtualizarTransacaoRequest(
        @NotNull UUID accountId,
        UUID categoryId,
        @Positive long valorCents,   // positivo; o service reaplica o sinal
        String descricao,
        @NotNull LocalDate data
) {}
