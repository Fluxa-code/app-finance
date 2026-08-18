package br.com.deivid.finance.usuario;

import br.com.deivid.finance.categoria.CategoriaRepository;
import br.com.deivid.finance.conta.ContaRepository;
import br.com.deivid.finance.fatura.FaturaRepository;
import br.com.deivid.finance.recorrencia.RecorrenciaRepository;
import br.com.deivid.finance.transacao.ParcelamentoRepository;
import br.com.deivid.finance.transacao.TransacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final TransacaoRepository transacoes;
    private final ParcelamentoRepository parcelamentos;
    private final RecorrenciaRepository recorrencias;
    private final FaturaRepository faturas;
    private final CategoriaRepository categorias;
    private final ContaRepository contas;

    public UsuarioService(UsuarioRepository usuarios,
                          PasswordEncoder encoder,
                          TransacaoRepository transacoes,
                          ParcelamentoRepository parcelamentos,
                          RecorrenciaRepository recorrencias,
                          FaturaRepository faturas,
                          CategoriaRepository categorias,
                          ContaRepository contas) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.transacoes = transacoes;
        this.parcelamentos = parcelamentos;
        this.recorrencias = recorrencias;
        this.faturas = faturas;
        this.categorias = categorias;
        this.contas = contas;
    }

    public PerfilResponse perfil(UUID userId) {
        Usuario u = buscar(userId);
        return new PerfilResponse(u.getId(), u.getEmail(), u.getNome(), u.getCreatedAt());
    }

    @Transactional
    public PerfilResponse atualizar(AtualizarPerfilRequest req, UUID userId) {
        Usuario u = buscar(userId);
        u.setNome(req.nome().trim());
        usuarios.save(u);
        return new PerfilResponse(u.getId(), u.getEmail(), u.getNome(), u.getCreatedAt());
    }

    @Transactional
    public void trocarSenha(TrocarSenhaRequest req, UUID userId) {
        Usuario u = buscar(userId);

        if (!encoder.matches(req.senhaAtual(), u.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha atual incorreta");
        }
        if (encoder.matches(req.novaSenha(), u.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A nova senha precisa ser diferente da atual");
        }

        u.setSenhaHash(encoder.encode(req.novaSenha()));
        usuarios.save(u);
    }

    /**
     * LGPD — direito à eliminação (art. 18, VI).
     *
     * Aqui NÃO é soft delete: é exclusão REAL, em cascata, dentro de UMA
     * transação. A ordem importa por causa das chaves estrangeiras (quem
     * aponta sai antes de quem é apontado). Ou apaga tudo, ou nada.
     *
     * Depois disso, os dados não existem mais em lugar nenhum do banco.
     * Só fica uma linha de log SEM dado pessoal (id + timestamp) — prova de
     * que a exclusão aconteceu, caso alguém questione.
     */
    @Transactional
    public void excluirConta(ExcluirContaRequest req, UUID userId) {
        Usuario u = buscar(userId);

        if (!encoder.matches(req.senha(), u.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha incorreta");
        }

        // ordem: folhas primeiro, raiz por último
        transacoes.deleteByUserId(userId);      // aponta pra contas, categorias, faturas, parcelamentos, recorrências
        recorrencias.deleteByUserId(userId);    // aponta pra contas, categorias
        faturas.deleteByUserId(userId);         // aponta pra contas
        parcelamentos.deleteByUserId(userId);   // aponta pra contas
        categorias.deleteByUserId(userId);
        contas.deleteByUserId(userId);
        usuarios.delete(u);

        log.info("LGPD: conta {} excluída em cascata", userId);
    }

    private Usuario buscar(UUID userId) {
        return usuarios.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }
}
