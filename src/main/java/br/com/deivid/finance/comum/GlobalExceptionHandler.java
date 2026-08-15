package br.com.deivid.finance.comum;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

/**
 * Intercepta as exceções de TODOS os controllers e transforma em JSON limpo.
 * Um lugar só decide como o erro sai da API — sem stack trace vazando.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // erros que o NOSSO código dispara de propósito (404, 409, 400 do service)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResponse> tratarResponseStatus(ResponseStatusException ex) {
        ErroResponse body = new ErroResponse(
                OffsetDateTime.now(),
                ex.getStatusCode().value(),
                ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    // falhas do @Valid — junta os erros de cada campo numa mensagem só
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErroResponse body = new ErroResponse(
                OffsetDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                mensagem);
        return ResponseEntity.badRequest().body(body);
    }
}
