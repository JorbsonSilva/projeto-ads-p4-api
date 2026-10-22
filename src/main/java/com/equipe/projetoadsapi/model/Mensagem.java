package com.equipe.projetoadsapi.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entidade de Mensagem do Chat interno.
 * Rastreabilidade: Fase 7 - f7-t1, f7-t3
 */
@Entity
@Table(name = "tb_mensagens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversa_id", nullable = false)
    private Conversa conversa;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "remetente_id", nullable = false)
    private Usuario remetente;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Builder.Default
    private Boolean lida = false;

    @Column(name = "lida_em")
    private Instant lidaEm;

    @Column(name = "enviada_em")
    private Instant enviadaEm;

    @PrePersist
    public void prePersist() {
        if (enviadaEm == null) enviadaEm = Instant.now();
        if (lida == null) lida = false;
    }
}
