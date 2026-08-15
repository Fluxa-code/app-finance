package br.com.deivid.finance.transacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TransacaoRepository extends JpaRepository<Transacao, UUID> {

    // extrato de um usuário (só o que não foi apagado)
    List<Transacao> findByUserIdAndDeletedAtIsNull(UUID userId);

    // transações de uma conta específica (pra calcular saldo, ver extrato da conta)
    List<Transacao> findByAccountIdAndDeletedAtIsNull(UUID accountId);

    // soma dos lançamentos de uma conta. COALESCE devolve 0 quando não há nenhum
    // (senão SUM de zero linhas retorna null).
    @Query("""
            SELECT COALESCE(SUM(t.valorCents), 0)
            FROM Transacao t
            WHERE t.accountId = :accountId AND t.deletedAt IS NULL
            """)
    long somarValorPorConta(@Param("accountId") UUID accountId);
}
