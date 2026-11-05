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
 * Rastreabilidade: Fase 6 - f6-t1, f6-t2, f6-t3, f6-t6 | Fase 7 - BACK-007
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estado oficial da sessão de aula sincronizado com o relógio do servidor (anti-fraude)")
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
    private Integer tempoRestanteSegundos;
    private Integer duracaoPrevistaSegundos;
    private Integer toleranciaRestanteSegundos;
    private Boolean alunoConfirmouFim;
    private Boolean professorConfirmouFim;
    private Instant timestampServidor;

    public static SessaoAulaDTO fromEntity(SessaoAula sessao) {
        if (sessao == null) return null;

        int duracaoPrevista = 3600; // 60 minutos padrão
        int pausado = sessao.getSegundosPausados() != null ? sessao.getSegundosPausados() : 0;
        int segundos = 0;

        if (sessao.getDuracaoTotalSegundos() != null && sessao.getDuracaoTotalSegundos() > 0) {
            segundos = sessao.getDuracaoTotalSegundos();
        } else if (sessao.getInicioEfetivo() != null) {
            if (sessao.getStatusSessao() == StatusSessao.EM_ANDAMENTO || sessao.getStatusSessao() == StatusSessao.FINALIZANDO) {
                long decorrido = Duration.between(sessao.getInicioEfetivo(), Instant.now()).getSeconds();
                segundos = (int) Math.max(0, decorrido - pausado);
            } else if (sessao.getStatusSessao() == StatusSessao.PAUSADA) {
                Instant refPausa = sessao.getUltimaPausaEm() != null ? sessao.getUltimaPausaEm() : Instant.now();
                long decorrido = Duration.between(sessao.getInicioEfetivo(), refPausa).getSeconds();
                segundos = (int) Math.max(0, decorrido - pausado);
            } else if (sessao.getStatusSessao() == StatusSessao.AGUARDANDO_CONFIRMACAO) {
                Instant refFim = sessao.getFimEfetivo() != null ? sessao.getFimEfetivo() : Instant.now();
                long decorrido = Duration.between(sessao.getInicioEfetivo(), refFim).getSeconds();
                segundos = (int) Math.max(0, decorrido - pausado);
            }
        }

        // Determinação do status semântico e tempo restante
        StatusSessao statusAtual = sessao.getStatusSessao();
        int tempoRestante = Math.max(0, duracaoPrevista - segundos);

        if (statusAtual == StatusSessao.CONCLUIDA) {
            tempoRestante = 0;
        } else if (statusAtual == StatusSessao.EM_ANDAMENTO && tempoRestante <= 300 && tempoRestante > 0) {
            statusAtual = StatusSessao.FINALIZANDO;
        }

        // Tolerância de 15 minutos (900s) se apenas 1 participante realizou check-in
        int toleranciaRestante = 0;
        if (statusAtual == StatusSessao.TOLERANCIA) {
            Instant refCheckin = sessao.getAlunoCheckinEm() != null ? sessao.getAlunoCheckinEm() : sessao.getProfessorCheckinEm();
            long decorridoTolerancia = refCheckin != null ? Duration.between(refCheckin, Instant.now()).getSeconds() : 0;
            toleranciaRestante = (int) Math.max(0, 900 - decorridoTolerancia);
        }

        return SessaoAulaDTO.builder()
                .id(sessao.getId())
                .agendamentoId(sessao.getAgendamento() != null ? sessao.getAgendamento().getId() : null)
                .alunoCheckin(sessao.getAlunoCheckin())
                .professorCheckin(sessao.getProfessorCheckin())
                .alunoCheckinEm(sessao.getAlunoCheckinEm())
                .professorCheckinEm(sessao.getProfessorCheckinEm())
                .statusSessao(statusAtual)
                .inicioEfetivo(sessao.getInicioEfetivo())
                .ultimaPausaEm(sessao.getUltimaPausaEm())
                .fimEfetivo(sessao.getFimEfetivo())
                .segundosDecorridos(segundos)
                .duracaoPrevistaSegundos(duracaoPrevista)
                .tempoRestanteSegundos(tempoRestante)
                .toleranciaRestanteSegundos(toleranciaRestante)
                .alunoConfirmouFim(sessao.getAlunoConfirmouFim())
                .professorConfirmouFim(sessao.getProfessorConfirmouFim())
                .timestampServidor(Instant.now())
                .build();
    }
}
