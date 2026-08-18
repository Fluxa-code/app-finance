package br.com.deivid.finance.usuario;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// /me = "eu, o usuário logado". Convenção comum em APIs (GitHub, Spotify usam).
@RestController
@RequestMapping("/me")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public PerfilResponse perfil(@AuthenticationPrincipal Jwt jwt) {
        return service.perfil(userId(jwt));
    }

    @PutMapping
    public PerfilResponse atualizar(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody AtualizarPerfilRequest req) {
        return service.atualizar(req, userId(jwt));
    }

    @PutMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(@AuthenticationPrincipal Jwt jwt,
                            @Valid @RequestBody TrocarSenhaRequest req) {
        service.trocarSenha(req, userId(jwt));
    }

    // DELETE com corpo (a senha). Alguns clientes HTTP estranham DELETE com body,
    // por isso muita API usa POST /me/excluir. Aqui DELETE é semanticamente certo
    // e o fetch do navegador aceita — mantemos.
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal Jwt jwt,
                        @Valid @RequestBody ExcluirContaRequest req) {
        service.excluirConta(req, userId(jwt));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
