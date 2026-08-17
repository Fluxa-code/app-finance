package br.com.deivid.finance.comum;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint de saúde: a plataforma bate aqui de tempos em tempos
 * pra saber se a aplicação está viva. Se parar de responder 200,
 * o Render reinicia o serviço.
 *
 * Público de propósito (não exige token) — monitoramento não faz login.
 * Também não devolve nada sensível: só "estou de pé".
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
