package br.com.deivid.finance.conta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import java.util.List;

public interface ContaRepository extends JpaRepository <Conta, UUID>{

    List<Conta> findByUserIdAndDeletedAtIsNull(UUID userId);

}
