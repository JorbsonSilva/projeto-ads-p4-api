package com.equipe.projetoadsapi.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Entidade de Ticket de Atendimento e Suporte Interno.
 * Rastreabilidade: Fase 7 - Bloco K (BACK-015)
 */
@Entity
@Table(name = "tb_tickets_suporte")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSuporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, unique = true, length = 50)
    private String protocolo;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 150)
    private String assunto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CategoriaTicket categoria;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private PrioridadeTicket prioridade = PrioridadeTicket.MEDIA;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private StatusTicket status = StatusTicket.ABERTO;

    @Column(columnDefinition = "TEXT")
    private String resposta;

    @Column(name = "resposta_admin", columnDefinition = "TEXT")
    private String respostaAdmin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respondido_por_id")
    private Usuario respondidoPor;

    @Column(name = "respondido_em")
    private Instant respondidoEm;

    @Column(name = "criado_em")
    private Instant criadoEm;

    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    @PrePersist
    public void prePersist() {
        if (criadoEm == null) criadoEm = Instant.now();
        if (atualizadoEm == null) atualizadoEm = Instant.now();
        if (status == null) status = StatusTicket.ABERTO;
        if (prioridade == null) prioridade = PrioridadeTicket.MEDIA;

        if (assunto == null && titulo != null) assunto = titulo;
        if (titulo == null && assunto != null) titulo = assunto;
        if (respostaAdmin == null && resposta != null) respostaAdmin = resposta;
        if (resposta == null && respostaAdmin != null) resposta = respostaAdmin;

        // Geração automática de protocolo oficial auditável SUP-YYYY-XXXXX
        if (protocolo == null || protocolo.isBlank()) {
            int ano = LocalDate.now().getYear();
            int seq = ThreadLocalRandom.current().nextInt(10000, 99999);
            protocolo = String.format("SUP-%d-%05d", ano, seq);
        }
    }

    @PreUpdate
    public void preUpdate() {
        atualizadoEm = Instant.now();
        if (respostaAdmin == null && resposta != null) respostaAdmin = resposta;
        if (resposta == null && respostaAdmin != null) resposta = respostaAdmin;
    }

    public String getAssunto() {
        return assunto != null ? assunto : titulo;
    }

    public String getRespostaAdmin() {
        return respostaAdmin != null ? respostaAdmin : resposta;
    }
}
