package br.com.deivid.finance.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmail(String email);
}
