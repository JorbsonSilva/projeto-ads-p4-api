package com.equipe.projetoadsapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entidade de Agendamento de Aulas do Orienta.
 * Rastreabilidade: Fase 2 (Baseline), Fase 4 (Padronização com StatusAgendamento)
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
}