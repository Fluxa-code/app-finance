package br.com.deivid.finance.categoria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CategoriaRequest(
        @NotNull UUID id,
        @NotBlank String nome,
        @NotNull TipoCategoria tipo,
        // cor em hex: #RGB ou #RRGGBB. Opcional.
        @Pattern(regexp = "^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$", message = "cor deve ser hex, ex: #FF5733")
        String cor
) {}
