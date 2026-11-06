package com.equipe.projetoadsapi.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Entidade de Denúncia e Moderação de Conduta com Protocolo de Ouvidoria.
 * Rastreabilidade: Fase 7 - Bloco K (BACK-016)
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

    @Column(unique = true, length = 50)
    private String protocolo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "denunciante_id", nullable = false)
    private Usuario denunciante;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "denunciado_id")
    private Usuario denunciado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id")
    private Agendamento agendamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mensagem_id")
    private Mensagem mensagem;

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

    @Column(name = "parecer_moderador", columnDefinition = "TEXT")
    private String parecerModerador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analisado_por_id")
    private Usuario analisadoPor;

    @Column(name = "criada_em")
    private Instant criadaEm;

    @Column(name = "analisada_em")
    private Instant analisadaEm;

    @Column(name = "data_atualizacao")
    private Instant dataAtualizacao;

    @PrePersist
    public void prePersist() {
        if (criadaEm == null) criadaEm = Instant.now();
        if (dataAtualizacao == null) dataAtualizacao = Instant.now();
        if (status == null) status = StatusDenuncia.PENDENTE;

        if (parecerModerador == null && parecerModeracao != null) parecerModerador = parecerModeracao;
        if (parecerModeracao == null && parecerModerador != null) parecerModeracao = parecerModerador;

        // Geração automática de protocolo auditável DEN-YYYY-XXXXX
        if (protocolo == null || protocolo.isBlank()) {
            int ano = LocalDate.now().getYear();
            int seq = ThreadLocalRandom.current().nextInt(10000, 99999);
            protocolo = String.format("DEN-%d-%05d", ano, seq);
        }
    }

    @PreUpdate
    public void preUpdate() {
        dataAtualizacao = Instant.now();
        if (parecerModerador == null && parecerModeracao != null) parecerModerador = parecerModeracao;
        if (parecerModeracao == null && parecerModerador != null) parecerModeracao = parecerModerador;
    }

    public String getParecerModerador() {
        return parecerModerador != null ? parecerModerador : parecerModeracao;
    }
}
