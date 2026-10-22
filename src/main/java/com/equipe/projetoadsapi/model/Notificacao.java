package com.equipe.projetoadsapi.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entidade de Notificação Interna do Orienta (Zero Custo).
 * Rastreabilidade: Fase 7 - f7-t5
 */
@Entity
@Table(name = "tb_notificacoes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoNotificacao tipo;

    @Builder.Default
    private Boolean lida = false;

    @Column(name = "data_hora")
    private Instant dataHora;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @PrePersist
    public void prePersist() {
        if (dataHora == null) dataHora = Instant.now();
        if (lida == null) lida = false;
    }
}
