package br.com.deivid.finance.categoria;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Categoria {

    @Id
    private UUID id;

    private UUID userId;

    private String nome;

    @Enumerated(EnumType.STRING)
    private TipoCategoria tipo;

    private String cor;   // hex pra UI: "#FF5733"

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    private OffsetDateTime deletedAt;
}
