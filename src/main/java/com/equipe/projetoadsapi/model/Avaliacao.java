package com.equipe.projetoadsapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entidade de Avaliação e Feedback pós-aula.
 * Rastreabilidade: Fase 6 - f6-t8
 */
@Entity
@Table(name = "tb_avaliacoes", uniqueConstraints = {
    @UniqueConstraint(name = "uk_avaliacao_agendamento_avaliador", columnNames = {"agendamento_id", "avaliador_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id", nullable = false)
    private Agendamento agendamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliador_id", nullable = false)
    private Usuario avaliador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliado_id", nullable = false)
    private Usuario avaliado;

    @Column(nullable = false)
    private Integer nota; // 1 a 5

    private Integer pontualidade; // 1 a 5

    private Integer didatica; // 1 a 5

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "criada_em")
    @Builder.Default
    private Instant criadaEm = Instant.now();
}
