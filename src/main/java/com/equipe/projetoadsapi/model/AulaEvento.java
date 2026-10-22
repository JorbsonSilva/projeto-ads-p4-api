package com.equipe.projetoadsapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entidade de auditoria e linha do tempo de eventos de uma aula ao vivo.
 * Rastreabilidade: Fase 6 - f6-t4, f6-t5
 */
@Entity
@Table(name = "tb_aula_eventos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AulaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessao_id", nullable = false)
    private SessaoAula sessao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 50)
    private TipoEventoAula tipoEvento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @Column(name = "timestamp_servidor", nullable = false)
    @Builder.Default
    private Instant timestampServidor = Instant.now();

    @Column(length = 255)
    private String motivo;

    @Column(name = "segundos_decorridos")
    @Builder.Default
    private Integer segundosDecorridos = 0;
}
