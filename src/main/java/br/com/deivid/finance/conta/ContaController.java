package br.com.deivid.finance.conta;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/contas")
public class ContaController {

    private final ContaRepository repository;

    public ContaController(ContaRepository repository){
        this.repository = repository;
    }

    @GetMapping
    public List<Conta> listar(){
        return repository.findAll();
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Conta criar(@Valid @RequestBody ContaRequest request){
        if (repository.existsById(request.id())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com esse id");
        }

        Conta conta = new Conta();
        conta.setId(request.id());
        conta.setUserId(request.userId());
        conta.setNome(request.nome());
        conta.setTipo(request.tipo());
        conta.setSaldoInicialCents(request.saldoInicialCents());
        conta.setLimiteCents(request.limiteCents());
        conta.setDiaFechamento(request.diaFechamento());
        conta.setDiaVencimento(request.diaVencimento());

        return repository.save(conta);
    }
}
