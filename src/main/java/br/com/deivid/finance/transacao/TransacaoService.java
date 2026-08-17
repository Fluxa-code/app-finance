package br.com.deivid.finance.transacao;

import br.com.deivid.finance.conta.ContaRepository;
import br.com.deivid.finance.eventos.EventoService;
import br.com.deivid.finance.fatura.FaturaService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TransacaoService {

    private final TransacaoRepository repository;
    private final ParcelamentoRepository parcelamentoRepository;
    private final FaturaService faturaService;
    private final EventoService eventos;
    private final ContaRepository contaRepository;

    public TransacaoService(TransacaoRepository repository,
                            ParcelamentoRepository parcelamentoRepository,
                            FaturaService faturaService,
                            EventoService eventos,
                            ContaRepository contaRepository) {
        this.repository = repository;
        this.parcelamentoRepository = parcelamentoRepository;
        this.faturaService = faturaService;
        this.eventos = eventos;
        this.contaRepository = contaRepository;
    }

    /** Transações do usuário logado, mais recentes primeiro. */
    public List<Transacao> listarTodas(UUID userId) {
        return repository.findByUserIdAndDeletedAtIsNullOrderByDataDescCreatedAtDesc(userId);
    }

    /** Extrato de uma conta — todos os lançamentos, do mais recente pro mais antigo. */
    public List<Transacao> extratoDaConta(UUID contaId, UUID userId) {
        return repository.findByAccountIdAndUserIdAndDeletedAtIsNullOrderByDataDesc(contaId, userId);
    }

    /**
     * Garante que a conta existe E pertence ao usuário. Conta alheia = 404,
     * não 403: pro estranho, ela nem existe.
     */
    private void garantirDono(UUID contaId, UUID userId) {
        boolean dono = contaRepository.findById(contaId)
                .filter(c -> c.getUserId().equals(userId))
                .isPresent();
        if (!dono) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada");
        }
    }

    /** Extrato de um mês do usuário (ex.: agosto/2026). */
    public List<Transacao> extratoDoMes(UUID userId, int ano, int mes) {
        if (mes < 1 || mes > 12) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Mês deve estar entre 1 e 12");
        }

        // monta o intervalo do mês: do dia 1 até o último dia
        LocalDate inicio = LocalDate.of(ano, mes, 1);
        LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());

        return repository.findByUserIdAndDataBetweenAndDeletedAtIsNullOrderByDataDesc(
                userId, inicio, fim);
    }

    /**
     * Cria uma transação simples: um gasto (SAIDA) ou uma receita (ENTRADA).
     * Transferência tem endpoint próprio.
     *
     * @Transactional AGORA: se for compra no cartão, além de salvar a transação
     * o método pode CRIAR uma fatura. Duas gravações → precisam ser atômicas.
     */
    @Transactional
    public Transacao criarSimples(TransacaoRequest req, UUID userId) {

        // transferência não passa por aqui — ela cria 2 linhas, é outro fluxo
        if (req.tipo() == TipoTransacao.TRANSFERENCIA) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Transferência deve usar o endpoint de transferência");
        }

        // a conta precisa ser SUA — sem isso, dava pra lançar na conta dos outros
        garantirDono(req.accountId(), userId);

        // id vem do cliente (offline-first) — se já existe, é conflito, não sobrescreve
        if (repository.existsById(req.id())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe transação com esse id");
        }

        // AQUI mora a regra: o cliente manda valor positivo, o service decide o sinal.
        // ENTRADA soma no saldo (+), SAIDA subtrai (-).
        long valorComSinal = req.tipo() == TipoTransacao.ENTRADA
                ? req.valorCents()
                : -req.valorCents();

        Transacao t = new Transacao();
        t.setId(req.id());
        t.setUserId(userId);            // do token — não mais do request
        t.setAccountId(req.accountId());
        t.setCategoryId(req.categoryId());
        t.setTipo(req.tipo());
        t.setValorCents(valorComSinal);
        t.setDescricao(req.descricao());
        t.setData(req.data());

        // se for cartão, descobre/cria a fatura e amarra a transação nela.
        // se não for cartão, resolverFatura devolve null e nada muda.
        t.setInvoiceId(faturaService.resolverFatura(req.accountId(), req.data()));

        Transacao salva = repository.save(t);
        eventos.publicar("transacao");   // avisa as telas conectadas
        return salva;
    }

    /**
     * Transfere dinheiro entre duas contas do usuário.
     * Gera DUAS transações irmãs (saída + entrada) ligadas pelo mesmo transferId.
     *
     * @Transactional: as duas nascem juntas ou nenhuma nasce. Se qualquer coisa
     * explodir no meio, o banco desfaz TUDO — nunca fica meia transferência.
     */
    @Transactional
    public List<Transacao> transferir(TransferenciaRequest req, UUID userId) {

        // não faz sentido transferir de uma conta pra ela mesma
        if (req.contaOrigemId().equals(req.contaDestinoId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Conta de origem e destino não podem ser a mesma");
        }

        // as DUAS pontas precisam ser suas
        garantirDono(req.contaOrigemId(), userId);
        garantirDono(req.contaDestinoId(), userId);

        // ids vêm do cliente — se algum já existe, é conflito
        if (repository.existsById(req.origemId()) || repository.existsById(req.destinoId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe transação com um desses ids");
        }

        // lado que SAI: valor negativo, na conta de origem
        Transacao saida = new Transacao();
        saida.setId(req.origemId());
        saida.setUserId(userId);
        saida.setAccountId(req.contaOrigemId());
        saida.setTipo(TipoTransacao.TRANSFERENCIA);
        saida.setValorCents(-req.valorCents());
        saida.setDescricao(req.descricao());
        saida.setData(req.data());
        saida.setTransferId(req.transferId());

        // lado que ENTRA: valor positivo, na conta de destino
        Transacao entrada = new Transacao();
        entrada.setId(req.destinoId());
        entrada.setUserId(userId);
        entrada.setAccountId(req.contaDestinoId());
        entrada.setTipo(TipoTransacao.TRANSFERENCIA);
        entrada.setValorCents(req.valorCents());
        entrada.setDescricao(req.descricao());
        entrada.setData(req.data());
        entrada.setTransferId(req.transferId());

        // saveAll grava as duas dentro da MESMA transação do banco
        List<Transacao> par = repository.saveAll(List.of(saida, entrada));
        eventos.publicar("transferencia");
        return par;
    }

    /**
     * Registra uma compra parcelada. Gera o cabeçalho (parcelamento) + N
     * transações, uma por mês, cada uma com a data da parcela.
     *
     * @Transactional: cabeçalho + N parcelas nascem juntos ou nada nasce.
     */
    @Transactional
    public List<Transacao> parcelar(ParcelamentoRequest req, UUID userId) {

        garantirDono(req.accountId(), userId);

        if (parcelamentoRepository.existsById(req.id())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe parcelamento com esse id");
        }

        // 1) o cabeçalho — a compra inteira, imutável
        Parcelamento cabecalho = new Parcelamento();
        cabecalho.setId(req.id());
        cabecalho.setUserId(userId);
        cabecalho.setAccountId(req.accountId());
        cabecalho.setDescricao(req.descricao());
        cabecalho.setValorTotalCents(req.valorTotalCents());
        cabecalho.setParcelaTotal(req.parcelaTotal());
        cabecalho.setDataPrimeira(req.dataPrimeira());
        parcelamentoRepository.save(cabecalho);

        // 2) divide o valor em centavos inteiros
        long base = req.valorTotalCents() / req.parcelaTotal();   // valor de cada parcela
        long resto = req.valorTotalCents() % req.parcelaTotal();  // o que sobra da divisão

        // 3) gera N parcelas, uma por mês
        List<Transacao> parcelas = new ArrayList<>();
        for (int i = 1; i <= req.parcelaTotal(); i++) {

            // a PRIMEIRA parcela leva o resto, pra soma bater exata
            long valorParcela = base + (i == 1 ? resto : 0);

            Transacao parcela = new Transacao();
            parcela.setId(UUID.randomUUID());   // simplificação: servidor gera (rever p/ offline)
            parcela.setUserId(userId);
            parcela.setAccountId(req.accountId());
            parcela.setCategoryId(req.categoryId());
            parcela.setTipo(TipoTransacao.SAIDA);
            parcela.setValorCents(-valorParcela);          // parcela é gasto: negativo
            parcela.setDescricao(req.descricao());
            parcela.setData(req.dataPrimeira().plusMonths(i - 1));  // parcela i cai no mês i
            parcela.setParcelamentoId(req.id());
            parcela.setParcelaNum(i);
            parcela.setParcelaTotal(req.parcelaTotal());

            // cada parcela cai na fatura do mês DELA (uma data diferente por parcela)
            parcela.setInvoiceId(faturaService.resolverFatura(req.accountId(), parcela.getData()));

            parcelas.add(parcela);
        }

        List<Transacao> salvas = repository.saveAll(parcelas);
        eventos.publicar("parcelamento");
        return salvas;
    }

    /**
     * Corrige um lançamento (valor, data, descrição, conta, categoria).
     *
     * Transferência e parcela NÃO são editáveis aqui: elas fazem parte de
     * um conjunto (2 linhas irmãs / N parcelas). Editar uma sozinha
     * quebraria a coerência do grupo — o certo é excluir e refazer.
     */
    @Transactional
    public Transacao atualizar(UUID id, AtualizarTransacaoRequest req, UUID userId) {

        Transacao t = buscarDoUsuario(id, userId);

        if (t.getTransferId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Transferência não pode ser editada. Exclua e lance de novo.");
        }
        if (t.getParcelamentoId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Parcela não pode ser editada. Exclua o parcelamento e lance de novo.");
        }

        garantirDono(req.accountId(), userId);

        t.setAccountId(req.accountId());
        t.setCategoryId(req.categoryId());
        t.setValorCents(t.getTipo() == TipoTransacao.ENTRADA
                ? req.valorCents()
                : -req.valorCents());
        t.setDescricao(req.descricao());
        t.setData(req.data());

        // a conta ou a data podem ter mudado -> a fatura pode ser outra
        t.setInvoiceId(faturaService.resolverFatura(req.accountId(), req.data()));

        Transacao salva = repository.save(t);
        eventos.publicar("transacao");
        return salva;
    }

    /**
     * Exclui um lançamento — SOFT DELETE (marca deletedAt, não apaga a linha).
     *
     * Se for parte de um grupo, o grupo INTEIRO sai junto:
     *  - transferência: as 2 linhas irmãs
     *  - parcelamento: as N parcelas
     * Apagar só um lado deixaria dinheiro aparecendo do nada.
     */
    @Transactional
    public void excluir(UUID id, UUID userId) {
        Transacao t = buscarDoUsuario(id, userId);

        List<Transacao> aExcluir;
        if (t.getTransferId() != null) {
            aExcluir = repository.findByTransferIdAndDeletedAtIsNull(t.getTransferId());
        } else if (t.getParcelamentoId() != null) {
            aExcluir = repository.findByParcelamentoIdAndDeletedAtIsNull(t.getParcelamentoId());
        } else {
            aExcluir = List.of(t);
        }

        OffsetDateTime agora = OffsetDateTime.now();
        aExcluir.forEach(x -> x.setDeletedAt(agora));
        repository.saveAll(aExcluir);

        eventos.publicar("transacao");
    }

    private Transacao buscarDoUsuario(UUID id, UUID userId) {
        return repository.findById(id)
                .filter(t -> t.getUserId().equals(userId))
                .filter(t -> t.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Lançamento não encontrado"));
    }
}
