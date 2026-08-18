package br.com.deivid.finance.fatura;

import br.com.deivid.finance.transacao.Transacao;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/faturas")
public class FaturaController {

    private final FaturaService service;

    public FaturaController(FaturaService service) {
        this.service = service;
    }

    // todas as faturas do usuário
    @GetMapping
    public List<FaturaResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return service.listar(userId(jwt));
    }

    // faturas de um cartão:  GET /faturas/cartao/{cardId}
    @GetMapping("/cartao/{cardId}")
    public List<FaturaResponse> doCartao(@AuthenticationPrincipal Jwt jwt,
                                         @PathVariable UUID cardId) {
        return service.listarDoCartao(cardId, userId(jwt));
    }

    // as compras de uma fatura
    @GetMapping("/{id}/itens")
    public List<Transacao> itens(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.itens(id, userId(jwt));
    }

    @PostMapping("/{id}/pagar")
    public FaturaResponse pagar(@AuthenticationPrincipal Jwt jwt,
                                @PathVariable UUID id,
                                @Valid @RequestBody PagarFaturaRequest req) {
        return service.pagar(id, req, userId(jwt));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
