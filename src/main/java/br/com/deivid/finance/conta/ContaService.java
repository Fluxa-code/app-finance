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

    /** Lista SÓ as contas do usuário logado. */
    public List<Conta> listar(UUID userId) {
        return contaRepository.findByUserIdAndDeletedAtIsNull(userId);
    }

    public Conta criar(ContaRequest request, UUID userId) {
        if (contaRepository.existsById(request.id())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Já existe uma conta com esse id");
        }

        Conta conta = new Conta();
        conta.setId(request.id());
        conta.setUserId(userId);   // do token — nunca do corpo da requisição
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
     *
     * Conta de OUTRO usuário responde 404 (e não 403): pro estranho, essa conta
     * simplesmente "não existe" — não confirmamos nem a existência dela.
     */
    public SaldoResponse saldo(UUID contaId, UUID userId) {
        Conta conta = contaRepository.findById(contaId)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Conta não encontrada"));

        long soma = transacaoRepository.somarValorPorConta(contaId);
        long saldoAtual = conta.getSaldoInicialCents() + soma;

        return new SaldoResponse(conta.getId(), conta.getNome(), saldoAtual);
    }
}
