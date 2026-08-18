package br.com.deivid.finance.recorrencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecorrenciaRepository extends JpaRepository<Recorrencia, UUID> {

    List<Recorrencia> findByUserIdAndDeletedAtIsNullOrderByDescricao(UUID userId);

    // o que o gerador diário processa: todas as regras vivas, de todo mundo
    List<Recorrencia> findByAtivaTrueAndDeletedAtIsNull();

    void deleteByUserId(UUID userId);
}
