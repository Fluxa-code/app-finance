package br.com.deivid.finance.categoria;

import java.util.UUID;

/**
 * Uma linha do relatório: quanto foi gasto numa categoria no período.
 *
 * categoriaId/nome/cor podem vir nulos — é o balde "Sem categoria",
 * dos lançamentos que o usuário não classificou.
 */
public record GastoPorCategoria(
        UUID categoriaId,
        String nome,
        String cor,
        long totalCents,      // positivo (o service inverte o sinal do gasto)
        long quantidade       // quantos lançamentos
) {}
