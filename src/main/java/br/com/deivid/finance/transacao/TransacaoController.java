package br.com.deivid.finance.transacao;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    public List<Transacao> listar() {
        return service.listarTodas();
    }

    // extrato de uma conta:  GET /transacoes/conta/{contaId}
    @GetMapping("/conta/{contaId}")
    public List<Transacao> extratoDaConta(@PathVariable UUID contaId) {
        return service.extratoDaConta(contaId);
    }

    // extrato do mês:  GET /transacoes/mes?userId=...&ano=2026&mes=8
    @GetMapping("/mes")
    public List<Transacao> extratoDoMes(@RequestParam UUID userId,
                                        @RequestParam int ano,
                                        @RequestParam int mes) {
        return service.extratoDoMes(userId, ano, mes);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transacao criar(@Valid @RequestBody TransacaoRequest req) {
        return service.criarSimples(req);
    }

    @PostMapping("/transferencias")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Transacao> transferir(@Valid @RequestBody TransferenciaRequest req) {
        return service.transferir(req);
    }

    @PostMapping("/parcelamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Transacao> parcelar(@Valid @RequestBody ParcelamentoRequest req) {
        return service.parcelar(req);
    }
}
