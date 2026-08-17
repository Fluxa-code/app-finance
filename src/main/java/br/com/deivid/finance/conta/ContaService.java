package br.com.deivid.finance.conta;

import br.com.deivid.finance.eventos.EventoService;
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
    private final EventoService eventos;

    public ContaService(ContaRepository contaRepository,
                        TransacaoRepository transacaoRepository,
                        EventoService eventos) {
        this.contaRepository = contaRepository;
        this.transacaoRepository = transacaoRepository;
        this.eventos = eventos;
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

        // cartão SEM dia de fechamento geraria erro lá na frente, na primeira
        // compra (quando a fatura tentasse nascer). Barramos aqui, na origem:
        // falhar CEDO com mensagem clara > falhar TARDE com erro obscuro.
        if (request.tipo() == TipoConta.CARTAO_CREDITO && request.diaFechamento() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cartão de crédito precisa do dia de fechamento");
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

        Conta salva = contaRepository.save(conta);
        eventos.publicar("conta");   // outras telas do usuário recarregam
        return salva;
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
