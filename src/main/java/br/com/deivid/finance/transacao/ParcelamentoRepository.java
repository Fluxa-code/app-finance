package br.com.deivid.finance.transacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ParcelamentoRepository extends JpaRepository<Parcelamento, UUID> {
}
