package br.com.deivid.finance.auth;

import br.com.deivid.finance.categoria.CategoriaService;
import br.com.deivid.finance.usuario.Usuario;
import br.com.deivid.finance.usuario.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final CategoriaService categorias;

    public AuthService(UsuarioRepository usuarios,
                       PasswordEncoder encoder,
                       JwtEncoder jwtEncoder,
                       CategoriaService categorias) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
        this.categorias = categorias;
    }

    // @Transactional: usuário + categorias nascem juntos ou nada nasce.
    // Sem isso, um erro no meio deixaria usuário sem as categorias iniciais.
    @Transactional
    public TokenResponse registrar(RegistroRequest req) {
        String email = req.email().toLowerCase().trim();

        if (usuarios.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Já existe uma conta com esse e-mail");
        }

        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());   // usuário nasce ONLINE por definição — servidor gera
        u.setEmail(email);
        u.setSenhaHash(encoder.encode(req.senha()));   // BCrypt: hash irreversível + salt
        u.setNome(req.nome());
        usuarios.save(u);

        // kit inicial: o app já nasce usável, sem a pessoa ter que configurar nada
        categorias.criarPadrao(u.getId());

        return gerarToken(u);
    }

    public TokenResponse login(LoginRequest req) {
        String email = req.email().toLowerCase().trim();

        // MESMA mensagem pra "e-mail não existe" e "senha errada", de propósito:
        // mensagem diferente deixaria alguém descobrir quais e-mails têm conta aqui.
        Usuario u = usuarios.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(this::credencialInvalida);

        if (!encoder.matches(req.senha(), u.getSenhaHash())) {
            throw credencialInvalida();
        }

        return gerarToken(u);
    }

    private ResponseStatusException credencialInvalida() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
    }

    /** Monta e assina o JWT. O "sub" (subject) carrega o id do usuário. */
    private TokenResponse gerarToken(Usuario u) {
        Instant agora = Instant.now();
        Instant expira = agora.plus(24, ChronoUnit.HOURS);   // dev: 24h. Prod: menor + refresh

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("fluxa")                    // quem emitiu
                .subject(u.getId().toString())      // DE QUEM é o token — vira o userId
                .claim("nome", u.getNome())
                .issuedAt(agora)
                .expiresAt(expira)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();

        return new TokenResponse(token, expira, u.getNome());
    }
}
