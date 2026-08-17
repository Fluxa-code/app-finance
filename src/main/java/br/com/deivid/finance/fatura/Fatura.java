package br.com.deivid.finance.fatura;

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
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
public class Fatura {

    @Id
    private UUID id;

    private UUID userId;

    private UUID cardId;          // o cartão (uma account do tipo CARTAO_CREDITO)

    private int ano;
    private int mes;              // a competência: "agosto/2026"

    private LocalDate dataFechamento;
    private LocalDate dataVencimento;

    @Enumerated(EnumType.STRING)
    private StatusFatura status;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    private OffsetDateTime deletedAt;
}
