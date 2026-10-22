package com.equipe.projetoadsapi.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entidade de Denúncia e Moderação de Conduta.
 * Rastreabilidade: Fase 7 - f7-t7
 */
@Entity
@Table(name = "tb_denuncias")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Denuncia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "denunciante_id", nullable = false)
    private Usuario denunciante;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "denunciado_id", nullable = false)
    private Usuario denunciado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id")
    private Agendamento agendamento;

    @Column(nullable = false, length = 100)
    private String motivo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private StatusDenuncia status = StatusDenuncia.PENDENTE;

    @Column(name = "parecer_moderacao", columnDefinition = "TEXT")
    private String parecerModeracao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analisado_por_id")
    private Usuario analisadoPor;

    @Column(name = "criada_em")
    private Instant criadaEm;

    @Column(name = "analisada_em")
    private Instant analisadaEm;

    @PrePersist
    public void prePersist() {
        if (criadaEm == null) criadaEm = Instant.now();
        if (status == null) status = StatusDenuncia.PENDENTE;
    }
}
