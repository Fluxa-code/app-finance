package br.com.deivid.finance.fatura;

import br.com.deivid.finance.conta.Conta;
import br.com.deivid.finance.conta.ContaRepository;
import br.com.deivid.finance.conta.TipoConta;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@Service
public class FaturaService {

    private final FaturaRepository faturaRepository;
    private final ContaRepository contaRepository;

    public FaturaService(FaturaRepository faturaRepository, ContaRepository contaRepository) {
        this.faturaRepository = faturaRepository;
        this.contaRepository = contaRepository;
    }

    /**
     * Descobre em qual fatura uma compra cai. Cria a fatura se ainda não existe.
     *
     * Devolve o id da fatura — ou null se a conta NÃO é cartão de crédito
     * (compra no débito/carteira não tem fatura).
     */
    public UUID resolverFatura(UUID contaId, LocalDate dataCompra) {

        Conta conta = contaRepository.findById(contaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Conta não encontrada"));

        // só cartão de crédito tem fatura
        if (conta.getTipo() != TipoConta.CARTAO_CREDITO) {
            return null;
        }

        Integer fechamento = conta.getDiaFechamento();
        if (fechamento == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cartão precisa ter dia de fechamento para gerar fatura");
        }

        // A REGRA: comprou até o dia de fechamento -> fatura DESTE mês.
        //          comprou depois do fechamento    -> fatura do mês SEGUINTE.
        YearMonth competencia = dataCompra.getDayOfMonth() <= fechamento
                ? YearMonth.from(dataCompra)
                : YearMonth.from(dataCompra).plusMonths(1);

        // acha a fatura da competência, ou cria uma nova
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

        // fatura fecha no dia de fechamento, DESTE mês
        fatura.setDataFechamento(diaNoMes(competencia, cartao.getDiaFechamento()));

        // vence no dia de vencimento, no mês SEGUINTE
        int diaVencimento = cartao.getDiaVencimento() != null
                ? cartao.getDiaVencimento()
                : cartao.getDiaFechamento();
        fatura.setDataVencimento(diaNoMes(competencia.plusMonths(1), diaVencimento));

        fatura.setStatus(StatusFatura.ABERTA);

        faturaRepository.save(fatura);
        return fatura.getId();
    }

    /** Monta uma data segura: se o dia não existe no mês (ex.: 31 em fevereiro), usa o último. */
    private LocalDate diaNoMes(YearMonth mes, int dia) {
        int diaValido = Math.min(dia, mes.lengthOfMonth());
        return mes.atDay(diaValido);
    }
}
