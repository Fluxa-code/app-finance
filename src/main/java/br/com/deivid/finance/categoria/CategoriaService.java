package br.com.deivid.finance.categoria;

import br.com.deivid.finance.transacao.TransacaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CategoriaService {

    private final CategoriaRepository repository;
    private final TransacaoRepository transacaoRepository;

    public CategoriaService(CategoriaRepository repository,
                            TransacaoRepository transacaoRepository) {
        this.repository = repository;
        this.transacaoRepository = transacaoRepository;
    }

    public List<Categoria> listar(UUID userId) {
        return repository.findByUserIdAndDeletedAtIsNullOrderByNome(userId);
    }

    public Categoria criar(CategoriaRequest req, UUID userId) {
        if (repository.existsById(req.id())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Já existe categoria com esse id");
        }

        Categoria c = new Categoria();
        c.setId(req.id());
        c.setUserId(userId);
        c.setNome(req.nome());
        c.setTipo(req.tipo());
        c.setCor(req.cor());

        return repository.save(c);
    }

    public Categoria atualizar(UUID id, CategoriaRequest req, UUID userId) {
        Categoria c = buscarDoUsuario(id, userId);
        c.setNome(req.nome());
        c.setTipo(req.tipo());
        c.setCor(req.cor());
        return repository.save(c);
    }

    /**
     * Exclui a categoria (soft delete).
     *
     * As transações que usavam ela NÃO são apagadas nem alteradas — elas
     * simplesmente passam a cair no balde "Sem categoria" do relatório.
     * Apagar histórico financeiro por causa de uma etiqueta seria destruir
     * dado do usuário.
     */
    public void excluir(UUID id, UUID userId) {
        Categoria c = buscarDoUsuario(id, userId);
        c.setDeletedAt(OffsetDateTime.now());
        repository.save(c);
    }

    /** Relatório: pra onde foi o dinheiro naquele mês. */
    public List<GastoPorCategoria> relatorioDoMes(UUID userId, int ano, int mes) {
        if (mes < 1 || mes > 12) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Mês deve estar entre 1 e 12");
        }
        LocalDate inicio = LocalDate.of(ano, mes, 1);
        LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());

        return transacaoRepository.gastosPorCategoria(userId, inicio, fim);
    }

    private Categoria buscarDoUsuario(UUID id, UUID userId) {
        return repository.findById(id)
                .filter(c -> c.getUserId().equals(userId))
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Categoria não encontrada"));
    }
}
