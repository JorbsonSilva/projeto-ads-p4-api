package com.equipe.projetoadsapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entidade que gerencia o ciclo ao vivo de uma aula.
 * Rastreabilidade: Fase 6 - f6-t1, f6-t2, f6-t3, f6-t6
 */
@Entity
@Table(name = "tb_sessao_aula")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessaoAula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agendamento_id", nullable = false, unique = true)
    private Agendamento agendamento;

    @Column(name = "aluno_checkin")
    @Builder.Default
    private Boolean alunoCheckin = false;

    @Column(name = "professor_checkin")
    @Builder.Default
    private Boolean professorCheckin = false;

    @Column(name = "aluno_checkin_em")
    private Instant alunoCheckinEm;

    @Column(name = "professor_checkin_em")
    private Instant professorCheckinEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_sessao", length = 30)
    @Builder.Default
    private StatusSessao statusSessao = StatusSessao.AGUARDANDO_CHECKIN;

    @Column(name = "inicio_efetivo")
    private Instant inicioEfetivo;

    @Column(name = "fim_efetivo")
    private Instant fimEfetivo;

    @Column(name = "segundos_pausados")
    @Builder.Default
    private Integer segundosPausados = 0;

    @Column(name = "ultima_pausa_em")
    private Instant ultimaPausaEm;

    @Column(name = "duracao_total_segundos")
    @Builder.Default
    private Integer duracaoTotalSegundos = 0;

    @Column(name = "aluno_confirmou_fim")
    @Builder.Default
    private Boolean alunoConfirmouFim = false;

    @Column(name = "professor_confirmou_fim")
    @Builder.Default
    private Boolean professorConfirmouFim = false;

    @Column(name = "criada_em")
    private Instant criadaEm;

    @Column(name = "atualizada_em")
    private Instant atualizadaEm;

    @PrePersist
    public void prePersist() {
        if (criadaEm == null) criadaEm = Instant.now();
        if (atualizadaEm == null) atualizadaEm = Instant.now();
        if (statusSessao == null) statusSessao = StatusSessao.AGUARDANDO_CHECKIN;
    }

    @PreUpdate
    public void preUpdate() {
        atualizadaEm = Instant.now();
    }
}
