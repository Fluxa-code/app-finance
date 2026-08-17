package br.com.deivid.finance.categoria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

    List<Categoria> findByUserIdAndDeletedAtIsNullOrderByNome(UUID userId);
}
