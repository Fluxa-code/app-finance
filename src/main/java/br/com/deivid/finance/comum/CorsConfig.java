package br.com.deivid.finance.comum;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Libera o front (que roda em outra porta) a chamar esta API.
 * DEV: libera o Vite em localhost:5173.
 * PROD: trocar pelo domínio real do site — nunca deixar liberado pra todos.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                       // todas as rotas
                .allowedOrigins("http://localhost:5173")  // o endereço do front no dev
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
    }
}
