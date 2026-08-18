package br.com.deivid.finance.recorrencia;

import br.com.deivid.finance.conta.ContaRepository;
import br.com.deivid.finance.eventos.EventoService;
import br.com.deivid.finance.fatura.FaturaService;
import br.com.deivid.finance.transacao.TipoTransacao;
import br.com.deivid.finance.transacao.Transacao;
import br.com.deivid.finance.transacao.TransacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RecorrenciaService {

    private static final Logger log = LoggerFactory.getLogger(RecorrenciaService.class);

    private final RecorrenciaRepository repository;
    private final TransacaoRepository transacoes;
    private final ContaRepository contas;
    private final FaturaService faturas;
    private final EventoService eventos;

    public RecorrenciaService(RecorrenciaRepository repository,
                              TransacaoRepository transacoes,
                              ContaRepository contas,
                              FaturaService faturas,
                              EventoService eventos) {
        this.repository = repository;
        this.transacoes = transacoes;
        this.contas = contas;
        this.faturas = faturas;
        this.eventos = eventos;
    }

    // ---------------- CRUD ----------------

    public List<Recorrencia> listar(UUID userId) {
        return repository.findByUserIdAndDeletedAtIsNullOrderByDescricao(userId);
    }

    @Transactional
    public Recorrencia criar(RecorrenciaRequest req, UUID userId) {
        if (req.tipo() == TipoTransacao.TRANSFERENCIA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Recorrência só pode ser entrada ou saída");
        }
        if (req.frequencia() == Frequencia.ANUAL && req.mes() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Recorrência anual precisa do mês");
        }
        if (req.frequencia() == Frequencia.SEMANAL && req.dia() > 7) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Recorrência semanal: dia deve ser 1 (segunda) a 7 (domingo)");
        }
        garantirDono(req.accountId(), userId);
        if (repository.existsById(req.id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe recorrência com esse id");
        }

        Recorrencia r = new Recorrencia();
        r.setId(req.id());
        r.setUserId(userId);
        r.setAccountId(req.accountId());
        r.setCategoryId(req.categoryId());
        r.setTipo(req.tipo());
        r.setValorCents(req.valorCents());
        r.setDescricao(req.descricao());
        r.setFrequencia(req.frequencia());
        r.setDia(req.dia());
        r.setMes(req.mes());
        r.setDataInicio(req.dataInicio());
        r.setDataFim(req.dataFim());
        r.setAtiva(true);
        repository.save(r);

        // gera imediatamente o que já venceu (ex.: começou dia 1, hoje é 20)
        // — assim o usuário vê efeito na hora, sem esperar o gerador da madrugada
        int geradas = gerarPara(r, LocalDate.now());
        if (geradas > 0) eventos.publicar("transacao");

        return r;
    }

    @Transactional
    public Recorrencia pausarOuRetomar(UUID id, UUID userId) {
        Recorrencia r = buscarDoUsuario(id, userId);
        r.setAtiva(!r.isAtiva());
        return repository.save(r);
    }

    /**
     * Exclui a regra e os lançamentos FUTUROS que ela gerou.
     * Os passados ficam: aconteceram de verdade, são histórico.
     */
    @Transactional
    public void excluir(UUID id, UUID userId) {
        Recorrencia r = buscarDoUsuario(id, userId);
        r.setDeletedAt(OffsetDateTime.now());
        r.setAtiva(false);
        repository.save(r);

        LocalDate hoje = LocalDate.now();
        List<Transacao> futuras = transacoes.findByRecurringRuleIdAndDeletedAtIsNull(id).stream()
                .filter(t -> t.getData().isAfter(hoje))
                .toList();
        futuras.forEach(t -> t.setDeletedAt(OffsetDateTime.now()));
        transacoes.saveAll(futuras);

        eventos.publicar("transacao");
    }

    // ---------------- O GERADOR ----------------

    /**
     * Roda uma vez por dia (ver RecorrenciaScheduler). Pra cada regra ativa,
     * cria os lançamentos que já deviam existir até hoje e ainda não existem.
     *
     * Idempotente: rodar 10x no mesmo dia gera a mesma coisa que rodar 1x,
     * porque `ultimaGeracao` marca até onde já fomos.
     */
    @Transactional
    public int gerarPendentes() {
        LocalDate hoje = LocalDate.now();
        int total = 0;
        for (Recorrencia r : repository.findByAtivaTrueAndDeletedAtIsNull()) {
            try {
                total += gerarPara(r, hoje);
            } catch (Exception e) {
                // uma regra quebrada não pode derrubar as outras
                log.error("Falha ao gerar recorrência {}: {}", r.getId(), e.getMessage());
            }
        }
        if (total > 0) {
            eventos.publicar("transacao");
            log.info("Recorrências: {} lançamento(s) gerado(s)", total);
        }
        return total;
    }

    /** Gera, pra UMA regra, tudo que vence entre a última geração e `ate`. */
    private int gerarPara(Recorrencia r, LocalDate ate) {
        // de onde partir: dia seguinte à última geração, ou o início da regra
        LocalDate cursor = r.getUltimaGeracao() != null
                ? r.getUltimaGeracao().plusDays(1)
                : r.getDataInicio();

        LocalDate limite = r.getDataFim() != null && r.getDataFim().isBefore(ate)
                ? r.getDataFim()
                : ate;

        List<Transacao> novas = new ArrayList<>();
        LocalDate ocorrencia = proximaOcorrencia(r, cursor);

        while (ocorrencia != null && !ocorrencia.isAfter(limite)) {
            novas.add(montar(r, ocorrencia));
            ocorrencia = proximaOcorrencia(r, ocorrencia.plusDays(1));
        }

        if (novas.isEmpty()) return 0;

        transacoes.saveAll(novas);
        r.setUltimaGeracao(limite);
        repository.save(r);
        return novas.size();
    }

    /**
     * A partir de `desde` (inclusive), qual a próxima data em que a regra ocorre?
     *
     * Detalhe do dia 31: regra "todo dia 31" em fevereiro cai no dia 28/29.
     * É o que a pessoa espera (o boleto vence no último dia útil).
     */
    private LocalDate proximaOcorrencia(Recorrencia r, LocalDate desde) {
        return switch (r.getFrequencia()) {
            case SEMANAL -> {
                DayOfWeek alvo = DayOfWeek.of(r.getDia());
                LocalDate d = desde;
                while (d.getDayOfWeek() != alvo) d = d.plusDays(1);
                yield d;
            }
            case MENSAL -> {
                YearMonth ym = YearMonth.from(desde);
                LocalDate cand = ym.atDay(Math.min(r.getDia(), ym.lengthOfMonth()));
                if (cand.isBefore(desde)) {
                    ym = ym.plusMonths(1);
                    cand = ym.atDay(Math.min(r.getDia(), ym.lengthOfMonth()));
                }
                yield cand;
            }
            case ANUAL -> {
                int ano = desde.getYear();
                LocalDate cand = dataAnual(ano, r.getMes(), r.getDia());
                if (cand.isBefore(desde)) cand = dataAnual(ano + 1, r.getMes(), r.getDia());
                yield cand;
            }
        };
    }

    private LocalDate dataAnual(int ano, int mes, int dia) {
        YearMonth ym = YearMonth.of(ano, mes);
        return ym.atDay(Math.min(dia, ym.lengthOfMonth()));
    }

    private Transacao montar(Recorrencia r, LocalDate data) {
        Transacao t = new Transacao();
        t.setId(UUID.randomUUID());
        t.setUserId(r.getUserId());
        t.setAccountId(r.getAccountId());
        t.setCategoryId(r.getCategoryId());
        t.setTipo(r.getTipo());
        t.setValorCents(r.getTipo() == TipoTransacao.ENTRADA ? r.getValorCents() : -r.getValorCents());
        t.setDescricao(r.getDescricao());
        t.setData(data);
        t.setRecurringRuleId(r.getId());
        t.setInvoiceId(faturas.resolverFatura(r.getAccountId(), data));
        return t;
    }

    // ---------------- helpers ----------------

    private void garantirDono(UUID contaId, UUID userId) {
        boolean dono = contas.findById(contaId).filter(c -> c.getUserId().equals(userId)).isPresent();
        if (!dono) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada");
    }

    private Recorrencia buscarDoUsuario(UUID id, UUID userId) {
        return repository.findById(id)
                .filter(r -> r.getUserId().equals(userId))
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recorrência não encontrada"));
    }
}
