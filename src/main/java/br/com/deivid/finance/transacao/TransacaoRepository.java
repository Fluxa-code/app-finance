package br.com.deivid.finance.transacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransacaoRepository extends JpaRepository<Transacao, UUID> {

    // extrato de um usuário (só o que não foi apagado)
    List<Transacao> findByUserIdAndDeletedAtIsNull(UUID userId);

    // transações de uma conta específica (pra calcular saldo, ver extrato da conta)
    List<Transacao> findByAccountIdAndDeletedAtIsNull(UUID accountId);
}
