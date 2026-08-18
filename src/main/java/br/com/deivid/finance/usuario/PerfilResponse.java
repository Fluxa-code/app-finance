package br.com.deivid.finance.usuario;

import java.time.OffsetDateTime;
import java.util.UUID;

// o que o usuário vê sobre si mesmo. Repara: sem senhaHash. NUNCA.
public record PerfilResponse(
        UUID id,
        String email,
        String nome,
        OffsetDateTime membroDesde
) {}
