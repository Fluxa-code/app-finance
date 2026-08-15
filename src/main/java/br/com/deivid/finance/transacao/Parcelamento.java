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
@Table(name = "parcelamentos")
@Getter
@Setter
@NoArgsConstructor
public class Parcelamento {

    @Id
    private UUID id;

    private UUID userId;

    private UUID accountId;

    private String descricao;

    private long valorTotalCents;   // a compra inteira (imutável)

    private int parcelaTotal;

    private LocalDate dataPrimeira;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    private OffsetDateTime deletedAt;
}
