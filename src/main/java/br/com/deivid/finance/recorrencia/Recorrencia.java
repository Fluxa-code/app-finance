package br.com.deivid.finance.recorrencia;

import br.com.deivid.finance.transacao.TipoTransacao;
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
@Table(name = "recurring_rules")
@Getter
@Setter
@NoArgsConstructor
public class Recorrencia {

    @Id
    private UUID id;

    private UUID userId;
    private UUID accountId;
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    private TipoTransacao tipo;      // só ENTRADA ou SAIDA

    private long valorCents;         // positivo; o sinal vem do tipo
    private String descricao;

    @Enumerated(EnumType.STRING)
    private Frequencia frequencia;

    private int dia;                 // dia do mês (MENSAL/ANUAL) ou 1-7 (SEMANAL, 1=segunda)
    private Integer mes;             // só ANUAL

    private LocalDate dataInicio;
    private LocalDate dataFim;       // null = sem fim

    private LocalDate ultimaGeracao; // marcador: até onde já geramos

    private boolean ativa;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    private OffsetDateTime deletedAt;
}
