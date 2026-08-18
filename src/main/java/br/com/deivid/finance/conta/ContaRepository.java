package br.com.deivid.finance.conta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContaRepository extends JpaRepository<Conta, UUID> {

    List<Conta> findByUserIdAndDeletedAtIsNull(UUID userId);

    void deleteByUserId(UUID userId);
}
