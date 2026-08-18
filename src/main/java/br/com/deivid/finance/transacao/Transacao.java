package br.com.deivid.finance.transacao;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transacao {

    @Id
    private UUID id;

    private UUID userId;

    private UUID accountId;

    private UUID categoryId;      // nullable: transferência não tem categoria

    @Enumerated(EnumType.STRING)
    private TipoTransacao tipo;

    private long valorCents;      // sinalizado: entra +, sai -

    private String descricao;

    private LocalDate data;       // coluna DATE (data sem hora) -> LocalDate

    // agrupa as 2 linhas de uma transferência
    private UUID transferId;

    // agrupa as N parcelas de uma compra parcelada
    private UUID parcelamentoId;
    private Integer parcelaNum;
    private Integer parcelaTotal;

    // em qual fatura essa compra de cartão caiu
    private UUID invoiceId;

    // se foi gerada por uma recorrência, qual
    private UUID recurringRuleId;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    private OffsetDateTime deletedAt;
}
