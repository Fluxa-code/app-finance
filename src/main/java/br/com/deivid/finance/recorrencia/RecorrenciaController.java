package br.com.deivid.finance.recorrencia;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/recorrencias")
public class RecorrenciaController {

    private final RecorrenciaService service;

    public RecorrenciaController(RecorrenciaService service) {
        this.service = service;
    }

    @GetMapping
    public List<Recorrencia> listar(@AuthenticationPrincipal Jwt jwt) {
        return service.listar(userId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Recorrencia criar(@AuthenticationPrincipal Jwt jwt,
                             @Valid @RequestBody RecorrenciaRequest req) {
        return service.criar(req, userId(jwt));
    }

    // PATCH: mudança parcial de estado (só o "ativa"), não substituição inteira
    @PatchMapping("/{id}/alternar")
    public Recorrencia alternar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.pausarOuRetomar(id, userId(jwt));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.excluir(id, userId(jwt));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
