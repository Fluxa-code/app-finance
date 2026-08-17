package br.com.deivid.finance.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    @Value("${app.jwt.secret}")
    private String segredo;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protege app de SESSÃO+COOKIE. Nossa API é stateless com
                // token no header — CSRF não se aplica, então desligamos.
                .csrf(csrf -> csrf.disable())

                // usa o bean de CORS (CorsConfig) — inclui o header Authorization
                .cors(Customizer.withDefaults())

                // NUNCA criar sessão no servidor. Cada request se prova sozinho
                // com o token. É isso que deixa a API escalável.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // login e registro são públicos por definição
                        .requestMatchers("/auth/**").permitAll()
                        // SSE fica aberto POR ENQUANTO: só transmite avisos
                        // "algo mudou", sem nenhum dado. Dívida: autenticar na etapa C.
                        .requestMatchers("/eventos").permitAll()
                        // health check da plataforma — monitoramento não faz login
                        .requestMatchers("/health").permitAll()
                        // 🔒 todo o resto exige token válido
                        .anyRequest().authenticated())

                // valida tokens JWT que chegarem no header Authorization: Bearer xxx
                .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()));

        return http.build();
    }

    /** BCrypt: hash de senha com salt embutido, lento de propósito. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private SecretKeySpec chave() {
        return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** Assina os tokens que a gente emite (login/registro). */
    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave()));
    }

    /** Confere a assinatura dos tokens que chegam nas requisições. */
    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(chave()).build();
    }
}
