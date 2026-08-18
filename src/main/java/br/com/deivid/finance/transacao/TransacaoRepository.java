package br.com.deivid.finance.transacao;

import br.com.deivid.finance.categoria.GastoPorCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransacaoRepository extends JpaRepository<Transacao, UUID> {

    // todas as transações do usuário (não apagadas), mais recentes primeiro
    List<Transacao> findByUserIdAndDeletedAtIsNullOrderByDataDescCreatedAtDesc(UUID userId);

    // extrato de uma conta DO USUÁRIO, mais recente primeiro.
    // O userId na query faz conta alheia devolver lista vazia — sem vazar nada.
    List<Transacao> findByAccountIdAndUserIdAndDeletedAtIsNullOrderByDataDesc(UUID accountId, UUID userId);

    // extrato de um mês (do usuário): tudo entre o 1º e o último dia, mais recente primeiro
    List<Transacao> findByUserIdAndDataBetweenAndDeletedAtIsNullOrderByDataDesc(
            UUID userId, LocalDate inicio, LocalDate fim);

    // as duas linhas irmãs de uma transferência (pra excluir juntas)
    List<Transacao> findByTransferIdAndDeletedAtIsNull(UUID transferId);

    // as N parcelas de uma compra (pra excluir juntas)
    List<Transacao> findByParcelamentoIdAndDeletedAtIsNull(UUID parcelamentoId);

    // lançamentos gerados por uma recorrência (pra limpar o futuro ao excluir a regra)
    List<Transacao> findByRecurringRuleIdAndDeletedAtIsNull(UUID recurringRuleId);

    // itens de uma fatura (as compras do cartão naquele período)
    List<Transacao> findByInvoiceIdAndDeletedAtIsNullOrderByDataDesc(UUID invoiceId);

    // total da fatura = soma das compras nela. Só SAIDA: o pagamento da fatura
    // (que é TRANSFERENCIA e entra positivo no cartão) NÃO reduz o "valor da fatura",
    // ele reduz o SALDO do cartão. São perguntas diferentes.
    @Query("""
            SELECT COALESCE(-SUM(t.valorCents), 0)
            FROM Transacao t
            WHERE t.invoiceId = :invoiceId
              AND t.deletedAt IS NULL
              AND t.tipo = br.com.deivid.finance.transacao.TipoTransacao.SAIDA
            """)
    long somarFatura(@Param("invoiceId") UUID invoiceId);

    @Query("""
            SELECT COUNT(t)
            FROM Transacao t
            WHERE t.invoiceId = :invoiceId
              AND t.deletedAt IS NULL
              AND t.tipo = br.com.deivid.finance.transacao.TipoTransacao.SAIDA
            """)
    long contarFatura(@Param("invoiceId") UUID invoiceId);

    // soma dos lançamentos de uma conta. COALESCE devolve 0 quando não há nenhum
    // (senão SUM de zero linhas retorna null).
    @Query("""
            SELECT COALESCE(SUM(t.valorCents), 0)
            FROM Transacao t
            WHERE t.accountId = :accountId AND t.deletedAt IS NULL
            """)
    long somarValorPorConta(@Param("accountId") UUID accountId);

    /**
     * Gastos do período agrupados por categoria.
     *
     * - só tipo SAIDA: transferência não é gasto, receita não entra aqui
     * - LEFT JOIN: quem não tem categoria vira o balde "Sem categoria"
     * - -SUM(...): o valor está negativo no banco; devolvemos positivo pro relatório
     * - ORDER BY: maior gasto primeiro, que é o que interessa ver
     */
    @Query("""
            SELECT new br.com.deivid.finance.categoria.GastoPorCategoria(
                       c.id, c.nome, c.cor, -SUM(t.valorCents), COUNT(t))
            FROM Transacao t
            LEFT JOIN Categoria c ON c.id = t.categoryId
            WHERE t.userId = :userId
              AND t.deletedAt IS NULL
              AND t.tipo = br.com.deivid.finance.transacao.TipoTransacao.SAIDA
              AND t.data BETWEEN :inicio AND :fim
            GROUP BY c.id, c.nome, c.cor
            ORDER BY SUM(t.valorCents) ASC
            """)
    List<GastoPorCategoria> gastosPorCategoria(@Param("userId") UUID userId,
                                               @Param("inicio") LocalDate inicio,
                                               @Param("fim") LocalDate fim);
}
