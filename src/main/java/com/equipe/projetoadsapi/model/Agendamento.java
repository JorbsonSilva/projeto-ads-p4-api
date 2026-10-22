package com.equipe.projetoadsapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entidade de Agendamento de Aulas do Orienta.
 * Rastreabilidade: Fase 2 (Baseline), Fase 4 (Padronização), Fase 5 (Evolução de Domínio)
 * Tarefas: f5-t3 (saber_id, assunto), f5-t4 (modalidade), f5-t5 (valor_aplicado), f5-t6 (StatusAgendamento)
 */
@Entity
@Table(name = "tb_agendamento")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_aluno", nullable = false)
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_professor", nullable = false)
    private Usuario professor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "saber_id")
    private Saber saber;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private LocalTime hora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatusAgendamento status = StatusAgendamento.PENDENTE;

    @Column(length = 500)
    private String justificativaCancelamento;

    @Column(length = 50)
    private String modalidade; // PRESENCIAL ou ONLINE

    @Column(length = 255)
    private String assunto;

    @Column(name = "valor_aplicado", precision = 10, scale = 2)
    private BigDecimal valorAplicado;

    @Column(name = "criado_em")
    private Instant criadoEm;

    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    @PrePersist
    public void prePersist() {
        if (criadoEm == null) criadoEm = Instant.now();
        if (atualizadoEm == null) atualizadoEm = Instant.now();
        if (status == null) status = StatusAgendamento.PENDENTE;
        if (valorAplicado == null) valorAplicado = BigDecimal.ZERO;
        if (modalidade == null) modalidade = "PRESENCIAL";
    }

    @PreUpdate
    public void preUpdate() {
        atualizadoEm = Instant.now();
    }
}