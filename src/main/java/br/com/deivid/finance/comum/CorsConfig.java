package br.com.deivid.finance.comum;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Quem pode chamar esta API.
 *
 * A lista vem da config (app.cors.origins): em dev é o Vite local,
 * em produção é o domínio do Vercel. Nunca "*" — isso deixaria
 * qualquer site do mundo chamar a API em nome do usuário logado.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.origins}")
    private List<String> origens;   // aceita valores separados por vírgula

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origens);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));   // inclui o Authorization do token

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
