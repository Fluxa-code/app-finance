package br.com.deivid.finance.categoria;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @GetMapping
    public List<Categoria> listar(@AuthenticationPrincipal Jwt jwt) {
        return service.listar(userId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Categoria criar(@AuthenticationPrincipal Jwt jwt,
                           @Valid @RequestBody CategoriaRequest req) {
        return service.criar(req, userId(jwt));
    }

    @PutMapping("/{id}")
    public Categoria atualizar(@AuthenticationPrincipal Jwt jwt,
                               @PathVariable UUID id,
                               @Valid @RequestBody CategoriaRequest req) {
        return service.atualizar(id, req, userId(jwt));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.excluir(id, userId(jwt));
    }

    // relatorio:  GET /categorias/relatorio?ano=2026&mes=8
    @GetMapping("/relatorio")
    public List<GastoPorCategoria> relatorio(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam int ano,
                                             @RequestParam int mes) {
        return service.relatorioDoMes(userId(jwt), ano, mes);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
