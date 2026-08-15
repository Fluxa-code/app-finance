package br.com.deivid.finance.conta;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @GetMapping
    public List<Conta> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Conta criar(@Valid @RequestBody ContaRequest request) {
        return service.criar(request);
    }

    @GetMapping("/{id}/saldo")
    public SaldoResponse saldo(@PathVariable UUID id) {
        return service.saldo(id);
    }
}
