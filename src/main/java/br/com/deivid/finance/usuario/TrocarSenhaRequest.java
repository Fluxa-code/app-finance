package br.com.deivid.finance.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Trocar senha exige a senha ATUAL. Motivo: se alguém pegar teu celular
 * destravado (ou roubar o token), não pode simplesmente trocar a senha e
 * te trancar pra fora. A senha atual é a prova de que é você mesmo.
 */
public record TrocarSenhaRequest(
        @NotBlank String senhaAtual,
        @NotBlank @Size(min = 8, message = "nova senha precisa ter pelo menos 8 caracteres") String novaSenha
) {}
