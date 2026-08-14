package br.com.deivid.finance.conta;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor

public class Conta {

    @Id
    private UUID id;

    private UUID userId;

    private String nome;

    @Enumerated(EnumType.STRING)
    private TipoConta tipo;

    private  long saldoInicialCents;

    private Long limiteCents;
    private Integer diaFechamento;
    private Integer diaVencimento;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
