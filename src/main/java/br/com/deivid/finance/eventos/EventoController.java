package br.com.deivid.finance.eventos;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class EventoController {

    private final EventoService service;

    public EventoController(EventoService service) {
        this.service = service;
    }

    // o navegador abre esta conexão e a deixa aberta, ouvindo
    @GetMapping("/eventos")
    public SseEmitter eventos() {
        return service.inscrever();
    }
}
