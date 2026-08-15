package br.com.deivid.finance.conta;

import br.com.deivid.finance.transacao.TransacaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ContaService {

    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;

    public ContaService(ContaRepository contaRepository,
                        TransacaoRepository transacaoRepository) {
        this.contaRepository = contaRepository;
        this.transacaoRepository = transacaoRepository;
    }

    public List<Conta> listar() {
        return contaRepository.findAll();
    }

    public Conta criar(ContaRequest request) {
        if (contaRepository.existsById(request.id())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Já existe uma conta com esse id");
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

        return contaRepository.save(conta);
    }

    /**
     * Saldo atual = saldo inicial da conta + soma de todos os lançamentos.
     * Cruza accounts + transactions — por isso mora no service, não no controller.
     */
    public SaldoResponse saldo(UUID contaId) {
        Conta conta = contaRepository.findById(contaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Conta não encontrada"));

        long soma = transacaoRepository.somarValorPorConta(contaId);
        long saldoAtual = conta.getSaldoInicialCents() + soma;

        return new SaldoResponse(conta.getId(), conta.getNome(), saldoAtual);
    }
}
