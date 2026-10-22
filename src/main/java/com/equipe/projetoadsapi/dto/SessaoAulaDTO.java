package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.SessaoAula;
import com.equipe.projetoadsapi.model.StatusSessao;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

/**
 * DTO para acompanhamento de aula ao vivo e cronômetro bilateral.
 * Rastreabilidade: Fase 6 - f6-t1, f6-t2, f6-t3, f6-t6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estado oficial da sessão de aula sincronizado com o relógio do servidor")
public class SessaoAulaDTO {

    private Long id;
    private Long agendamentoId;
    private Boolean alunoCheckin;
    private Boolean professorCheckin;
    private Instant alunoCheckinEm;
    private Instant professorCheckinEm;
    private StatusSessao statusSessao;
    private Instant inicioEfetivo;
    private Instant ultimaPausaEm;
    private Instant fimEfetivo;
    private Integer segundosDecorridos;
    private Boolean alunoConfirmouFim;
    private Boolean professorConfirmouFim;
    private Instant timestampServidor;

    public static SessaoAulaDTO fromEntity(SessaoAula sessao) {
        if (sessao == null) return null;

        int segundos = sessao.getDuracaoTotalSegundos() != null ? sessao.getDuracaoTotalSegundos() : 0;
        int pausado = sessao.getSegundosPausados() != null ? sessao.getSegundosPausados() : 0;

        if (sessao.getInicioEfetivo() != null) {
            if (sessao.getStatusSessao() == StatusSessao.EM_ANDAMENTO) {
                long decorrido = Duration.between(sessao.getInicioEfetivo(), Instant.now()).getSeconds();
                segundos = (int) Math.max(0, decorrido - pausado);
            } else if (sessao.getStatusSessao() == StatusSessao.PAUSADA) {
                Instant refPausa = sessao.getUltimaPausaEm() != null ? sessao.getUltimaPausaEm() : Instant.now();
                long decorrido = Duration.between(sessao.getInicioEfetivo(), refPausa).getSeconds();
                segundos = (int) Math.max(0, decorrido - pausado);
            }
        }

        return SessaoAulaDTO.builder()
                .id(sessao.getId())
                .agendamentoId(sessao.getAgendamento() != null ? sessao.getAgendamento().getId() : null)
                .alunoCheckin(sessao.getAlunoCheckin())
                .professorCheckin(sessao.getProfessorCheckin())
                .alunoCheckinEm(sessao.getAlunoCheckinEm())
                .professorCheckinEm(sessao.getProfessorCheckinEm())
                .statusSessao(sessao.getStatusSessao())
                .inicioEfetivo(sessao.getInicioEfetivo())
                .ultimaPausaEm(sessao.getUltimaPausaEm())
                .fimEfetivo(sessao.getFimEfetivo())
                .segundosDecorridos(segundos)
                .alunoConfirmouFim(sessao.getAlunoConfirmouFim())
                .professorConfirmouFim(sessao.getProfessorConfirmouFim())
                .timestampServidor(Instant.now())
                .build();
    }
}
