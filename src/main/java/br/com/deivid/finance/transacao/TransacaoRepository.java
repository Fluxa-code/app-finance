package br.com.deivid.finance.transacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransacaoRepository extends JpaRepository<Transacao, UUID> {

    // todas as transações do usuário (não apagadas)
    List<Transacao> findByUserIdAndDeletedAtIsNull(UUID userId);

    // extrato de uma conta DO USUÁRIO, mais recente primeiro.
    // O userId na query faz conta alheia devolver lista vazia — sem vazar nada.
    List<Transacao> findByAccountIdAndUserIdAndDeletedAtIsNullOrderByDataDesc(UUID accountId, UUID userId);

    // extrato de um mês (do usuário): tudo entre o 1º e o último dia, mais recente primeiro
    List<Transacao> findByUserIdAndDataBetweenAndDeletedAtIsNullOrderByDataDesc(
            UUID userId, LocalDate inicio, LocalDate fim);

    // soma dos lançamentos de uma conta. COALESCE devolve 0 quando não há nenhum
    // (senão SUM de zero linhas retorna null).
    @Query("""
            SELECT COALESCE(SUM(t.valorCents), 0)
            FROM Transacao t
            WHERE t.accountId = :accountId AND t.deletedAt IS NULL
            """)
    long somarValorPorConta(@Param("accountId") UUID accountId);
}
