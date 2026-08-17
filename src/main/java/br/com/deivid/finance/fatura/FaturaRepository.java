package br.com.deivid.finance.fatura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FaturaRepository extends JpaRepository<Fatura, UUID> {

    // acha a fatura de um cartão numa competência (mês/ano) específica
    Optional<Fatura> findByCardIdAndAnoAndMesAndDeletedAtIsNull(UUID cardId, int ano, int mes);
}
