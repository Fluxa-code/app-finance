package br.com.deivid.finance.fatura;

import br.com.deivid.finance.conta.Conta;
import br.com.deivid.finance.conta.ContaRepository;
import br.com.deivid.finance.conta.TipoConta;
import br.com.deivid.finance.eventos.EventoService;
import br.com.deivid.finance.transacao.TipoTransacao;
import br.com.deivid.finance.transacao.Transacao;
import br.com.deivid.finance.transacao.TransacaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class FaturaService {

    private final FaturaRepository faturaRepository;
    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;
    private final EventoService eventos;

    public FaturaService(FaturaRepository faturaRepository,
                         ContaRepository contaRepository,
                         TransacaoRepository transacaoRepository,
                         EventoService eventos) {
        this.faturaRepository = faturaRepository;
        this.contaRepository = contaRepository;
        this.transacaoRepository = transacaoRepository;
        this.eventos = eventos;
    }

    // ======================================================
    // RESOLVER (já existia): em qual fatura uma compra cai
    // ======================================================

    public UUID resolverFatura(UUID contaId, LocalDate dataCompra) {
        Conta conta = contaRepository.findById(contaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Conta não encontrada"));

        if (conta.getTipo() != TipoConta.CARTAO_CREDITO) {
            return null;
        }

        Integer fechamento = conta.getDiaFechamento();
        if (fechamento == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cartão precisa ter dia de fechamento para gerar fatura");
        }

        YearMonth competencia = dataCompra.getDayOfMonth() <= fechamento
                ? YearMonth.from(dataCompra)
                : YearMonth.from(dataCompra).plusMonths(1);

        return faturaRepository
                .findByCardIdAndAnoAndMesAndDeletedAtIsNull(
                        contaId, competencia.getYear(), competencia.getMonthValue())
                .map(Fatura::getId)
                .orElseGet(() -> criarFatura(conta, competencia));
    }

    private UUID criarFatura(Conta cartao, YearMonth competencia) {
        Fatura fatura = new Fatura();
        fatura.setId(UUID.randomUUID());
        fatura.setUserId(cartao.getUserId());
        fatura.setCardId(cartao.getId());
        fatura.setAno(competencia.getYear());
        fatura.setMes(competencia.getMonthValue());
        fatura.setDataFechamento(diaNoMes(competencia, cartao.getDiaFechamento()));

        int diaVencimento = cartao.getDiaVencimento() != null
                ? cartao.getDiaVencimento()
                : cartao.getDiaFechamento();
        fatura.setDataVencimento(diaNoMes(competencia.plusMonths(1), diaVencimento));
        fatura.setStatus(StatusFatura.ABERTA);

        faturaRepository.save(fatura);
        return fatura.getId();
    }

    private LocalDate diaNoMes(YearMonth mes, int dia) {
        return mes.atDay(Math.min(dia, mes.lengthOfMonth()));
    }

    // ======================================================
    // LER: as faturas como a tela vê
    // ======================================================

    /** Todas as faturas do usuário (todos os cartões), com total calculado. */
    public List<FaturaResponse> listar(UUID userId) {
        return faturaRepository.findByUserIdAndDeletedAtIsNullOrderByAnoDescMesDesc(userId)
                .stream()
                .map(this::montarResponse)
                .toList();
    }

    /** Faturas de um cartão específico. */
    public List<FaturaResponse> listarDoCartao(UUID cardId, UUID userId) {
        garantirCartaoDoUsuario(cardId, userId);
        return faturaRepository.findByCardIdAndDeletedAtIsNullOrderByAnoDescMesDesc(cardId)
                .stream()
                .map(this::montarResponse)
                .toList();
    }

    /** As compras dentro de uma fatura. */
    public List<Transacao> itens(UUID faturaId, UUID userId) {
        buscarDoUsuario(faturaId, userId);
        return transacaoRepository.findByInvoiceIdAndDeletedAtIsNullOrderByDataDesc(faturaId);
    }

    /**
     * Status DERIVADO da data, não do que está gravado.
     *
     * Motivo: se dependesse de alguém "fechar" a fatura, precisaria de um job
     * mudando status todo dia — e um dia atrasado mostraria fatura ABERTA
     * depois de fechada. Calcular pela data é sempre correto, sem job.
     * PAGA é a única que é decisão do usuário, então essa é lida do banco.
     */
    private StatusFatura statusDe(Fatura f) {
        if (f.getStatus() == StatusFatura.PAGA) return StatusFatura.PAGA;
        return LocalDate.now().isAfter(f.getDataFechamento())
                ? StatusFatura.FECHADA
                : StatusFatura.ABERTA;
    }

    private FaturaResponse montarResponse(Fatura f) {
        String nomeCartao = contaRepository.findById(f.getCardId())
                .map(Conta::getNome).orElse("Cartão");
        return new FaturaResponse(
                f.getId(), f.getCardId(), nomeCartao,
                f.getAno(), f.getMes(),
                f.getDataFechamento(), f.getDataVencimento(),
                statusDe(f),
                transacaoRepository.somarFatura(f.getId()),
                transacaoRepository.contarFatura(f.getId()));
    }

    // ======================================================
    // PAGAR: transferência conta -> cartão + marca como PAGA
    // ======================================================

    /**
     * Pagar fatura NÃO é um gasto novo — os gastos foram as compras, lá atrás
     * (regime de competência). Pagar é só o dinheiro trocando de bolso:
     * sai da corrente, entra no cartão, zerando a dívida (regime de caixa).
     *
     * Por isso é uma TRANSFERÊNCIA — a mesma mecânica que já existe.
     * Contar de novo aqui seria dobrar o gasto no relatório.
     */
    @Transactional
    public FaturaResponse pagar(UUID faturaId, PagarFaturaRequest req, UUID userId) {
        Fatura fatura = buscarDoUsuario(faturaId, userId);

        if (fatura.getStatus() == StatusFatura.PAGA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Fatura já está paga");
        }

        Conta origem = contaRepository.findById(req.contaOrigemId())
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Conta de origem não encontrada"));

        if (origem.getTipo() == TipoConta.CARTAO_CREDITO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não dá pra pagar fatura com outro cartão de crédito");
        }
        if (origem.getId().equals(fatura.getCardId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Conta de origem não pode ser o próprio cartão");
        }

        UUID transferId = UUID.randomUUID();
        String descricao = "Pagamento fatura " + String.format("%02d/%d", fatura.getMes(), fatura.getAno());

        Transacao saida = new Transacao();
        saida.setId(UUID.randomUUID());
        saida.setUserId(userId);
        saida.setAccountId(origem.getId());
        saida.setTipo(TipoTransacao.TRANSFERENCIA);
        saida.setValorCents(-req.valorCents());
        saida.setDescricao(descricao);
        saida.setData(req.data());
        saida.setTransferId(transferId);

        Transacao entrada = new Transacao();
        entrada.setId(UUID.randomUUID());
        entrada.setUserId(userId);
        entrada.setAccountId(fatura.getCardId());
        entrada.setTipo(TipoTransacao.TRANSFERENCIA);
        entrada.setValorCents(req.valorCents());
        entrada.setDescricao(descricao);
        entrada.setData(req.data());
        entrada.setTransferId(transferId);
        entrada.setInvoiceId(fatura.getId());   // o pagamento aparece dentro da fatura

        transacaoRepository.saveAll(List.of(saida, entrada));

        // pagou o valor cheio (ou mais)? marca PAGA. Parcial fica como está.
        long total = transacaoRepository.somarFatura(fatura.getId());
        if (req.valorCents() >= total) {
            fatura.setStatus(StatusFatura.PAGA);
            faturaRepository.save(fatura);
        }

        eventos.publicar("transacao");
        return montarResponse(fatura);
    }

    // ======================================================
    // helpers
    // ======================================================

    private Fatura buscarDoUsuario(UUID id, UUID userId) {
        return faturaRepository.findById(id)
                .filter(f -> f.getUserId().equals(userId))
                .filter(f -> f.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Fatura não encontrada"));
    }

    private void garantirCartaoDoUsuario(UUID cardId, UUID userId) {
        boolean ok = contaRepository.findById(cardId)
                .filter(c -> c.getUserId().equals(userId))
                .filter(c -> c.getTipo() == TipoConta.CARTAO_CREDITO)
                .isPresent();
        if (!ok) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cartão não encontrado");
    }
}
