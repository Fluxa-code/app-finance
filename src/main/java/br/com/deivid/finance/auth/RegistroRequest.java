package br.com.deivid.finance.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "senha precisa ter pelo menos 8 caracteres") String senha,
        @NotBlank String nome
) {}
