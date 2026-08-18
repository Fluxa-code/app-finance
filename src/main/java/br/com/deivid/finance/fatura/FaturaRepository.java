package br.com.deivid.finance.fatura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FaturaRepository extends JpaRepository<Fatura, UUID> {

    // acha a fatura de um cartão numa competência (mês/ano) específica
    Optional<Fatura> findByCardIdAndAnoAndMesAndDeletedAtIsNull(UUID cardId, int ano, int mes);

    // todas as faturas de um cartão, mais recente primeiro
    List<Fatura> findByCardIdAndDeletedAtIsNullOrderByAnoDescMesDesc(UUID cardId);

    // todas as faturas do usuário (todos os cartões)
    List<Fatura> findByUserIdAndDeletedAtIsNullOrderByAnoDescMesDesc(UUID userId);

    void deleteByUserId(UUID userId);
}
