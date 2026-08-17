package br.com.deivid.finance.eventos;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Canal de tempo real via SSE (Server-Sent Events).
 *
 * Cada navegador aberto se inscreve (GET /eventos) e fica com uma conexão
 * HTTP aberta. Quando algo muda no servidor, a gente empurra um aviso por
 * essa conexão — e cada tela decide recarregar seus dados.
 */
@Service
public class EventoService {

    // lista thread-safe: navegadores entram e saem a qualquer momento
    private final List<SseEmitter> inscritos = new CopyOnWriteArrayList<>();

    public SseEmitter inscrever() {
        SseEmitter emitter = new SseEmitter(0L);   // 0 = conexão sem timeout
        inscritos.add(emitter);

        // quando a conexão morre (aba fechou, rede caiu), sai da lista
        emitter.onCompletion(() -> inscritos.remove(emitter));
        emitter.onTimeout(() -> inscritos.remove(emitter));
        emitter.onError(e -> inscritos.remove(emitter));

        return emitter;
    }

    /** Avisa TODOS os navegadores conectados que algo mudou. */
    public void publicar(String oQueMudou) {
        for (SseEmitter emitter : inscritos) {
            try {
                emitter.send(SseEmitter.event().name("mudanca").data(oQueMudou));
            } catch (IOException e) {
                emitter.completeWithError(e);   // dispara onError -> remove da lista
            }
        }
    }
}
