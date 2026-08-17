package br.com.deivid.finance.transacao;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transacoes")
public class TransacaoController {

    private final TransacaoService service;

    public TransacaoController(TransacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Transacao> listar(@AuthenticationPrincipal Jwt jwt) {
        return service.listarTodas(userId(jwt));
    }

    // extrato de uma conta:  GET /transacoes/conta/{contaId}
    @GetMapping("/conta/{contaId}")
    public List<Transacao> extratoDaConta(@AuthenticationPrincipal Jwt jwt,
                                          @PathVariable UUID contaId) {
        return service.extratoDaConta(contaId, userId(jwt));
    }

    // extrato do mês:  GET /transacoes/mes?ano=2026&mes=8  (userId agora vem do token!)
    @GetMapping("/mes")
    public List<Transacao> extratoDoMes(@AuthenticationPrincipal Jwt jwt,
                                        @RequestParam int ano,
                                        @RequestParam int mes) {
        return service.extratoDoMes(userId(jwt), ano, mes);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transacao criar(@AuthenticationPrincipal Jwt jwt,
                           @Valid @RequestBody TransacaoRequest req) {
        return service.criarSimples(req, userId(jwt));
    }

    @PostMapping("/transferencias")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Transacao> transferir(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody TransferenciaRequest req) {
        return service.transferir(req, userId(jwt));
    }

    @PostMapping("/parcelamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Transacao> parcelar(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody ParcelamentoRequest req) {
        return service.parcelar(req, userId(jwt));
    }

    @PutMapping("/{id}")
    public Transacao atualizar(@AuthenticationPrincipal Jwt jwt,
                               @PathVariable UUID id,
                               @Valid @RequestBody AtualizarTransacaoRequest req) {
        return service.atualizar(id, req, userId(jwt));
    }

    // 204 No Content: deu certo e não há corpo pra devolver
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.excluir(id, userId(jwt));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
