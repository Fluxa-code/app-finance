package br.com.deivid.finance.transacao;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
