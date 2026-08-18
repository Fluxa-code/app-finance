package br.com.deivid.finance.recorrencia;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Acorda o gerador de recorrências.
 *
 * Roda às 03:05 (horário de Brasília) todos os dias — hora morta, pouca gente
 * usando — E também logo que a aplicação sobe. O segundo é importante no free
 * tier: o servidor "dorme" e pode não estar acordado às 03:05. Quando alguém
 * o acorda, ele corre atrás do prejuízo na hora.
 *
 * Como o gerador é idempotente (não duplica), rodar "a mais" não faz mal.
 */
@Component
public class RecorrenciaScheduler {

    private final RecorrenciaService service;

    public RecorrenciaScheduler(RecorrenciaService service) {
        this.service = service;
    }

    // cron: seg min hora dia mês dia-da-semana
    @Scheduled(cron = "0 5 3 * * *", zone = "America/Sao_Paulo")
    public void diario() {
        service.gerarPendentes();
    }

    // 15s após subir, e depois a cada 6h (rede de segurança pro cold start)
    @Scheduled(initialDelay = 15_000, fixedDelay = 6 * 60 * 60 * 1000)
    public void aoSubirEPeriodico() {
        service.gerarPendentes();
    }
}
