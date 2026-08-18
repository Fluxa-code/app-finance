package br.com.deivid.finance.usuario;

import jakarta.validation.constraints.NotBlank;

public record AtualizarPerfilRequest(
        @NotBlank String nome
) {}
