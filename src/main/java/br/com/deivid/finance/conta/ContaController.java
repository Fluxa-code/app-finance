package br.com.deivid.finance.conta;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/contas")
public class ContaController {

    private final ContaService service;

    public ContaController(ContaService service) {
        this.service = service;
    }

    // @AuthenticationPrincipal Jwt = o token JÁ VALIDADO pelo filtro de segurança.
    // jwt.getSubject() é o "sub" — o id do usuário dono do token.
    @GetMapping
    public List<Conta> listar(@AuthenticationPrincipal Jwt jwt) {
        return service.listar(userId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Conta criar(@AuthenticationPrincipal Jwt jwt,
                       @Valid @RequestBody ContaRequest request) {
        return service.criar(request, userId(jwt));
    }

    @GetMapping("/{id}/saldo")
    public SaldoResponse saldo(@AuthenticationPrincipal Jwt jwt,
                               @PathVariable UUID id) {
        return service.saldo(id, userId(jwt));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
