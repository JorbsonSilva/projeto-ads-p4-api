package com.equipe.projetoadsapi.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entidade de Conversa de Chat restrita a um agendamento específico.
 * Rastreabilidade: Fase 7 - f7-t1, f7-t2
 */
@Entity
@Table(name = "tb_conversas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agendamento_id", nullable = false, unique = true)
    private Agendamento agendamento;

    @Builder.Default
    private Boolean ativa = true;

    @Column(name = "criada_em")
    private Instant criadaEm;

    @Column(name = "atualizada_em")
    private Instant atualizadaEm;

    @PrePersist
    public void prePersist() {
        if (criadaEm == null) criadaEm = Instant.now();
        if (atualizadaEm == null) atualizadaEm = Instant.now();
        if (ativa == null) ativa = true;
    }

    @PreUpdate
    public void preUpdate() {
        atualizadaEm = Instant.now();
    }
}
