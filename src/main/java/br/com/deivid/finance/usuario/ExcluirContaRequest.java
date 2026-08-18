package br.com.deivid.finance.usuario;

import jakarta.validation.constraints.NotBlank;

/**
 * Excluir a conta é IRREVERSÍVEL. Exigir a senha é a fricção certa:
 * impede exclusão acidental e impede que um token vazado apague tudo.
 */
public record ExcluirContaRequest(
        @NotBlank String senha
) {}
