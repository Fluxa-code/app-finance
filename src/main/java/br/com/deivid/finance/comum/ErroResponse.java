package br.com.deivid.finance.comum;

import java.time.OffsetDateTime;

// O formato ÚNICO de erro que a API devolve. Sem stack trace, sem tripas.
public record ErroResponse(
        OffsetDateTime timestamp,
        int status,
        String mensagem
) {}
