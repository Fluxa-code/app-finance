package br.com.deivid.finance.comum;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Libera o front (que roda em outra porta) a chamar esta API.
 * Agora como CorsConfigurationSource: é este bean que o Spring SECURITY
 * usa — o formato antigo (WebMvcConfigurer) não vale pra requisições
 * que passam pelo filtro de segurança.
 *
 * DEV: localhost:5173. PROD: trocar pelo domínio real do site.
 */
@Configuration
public class CorsConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // "*" inclui Content-Type e, principalmente, o Authorization do token
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
