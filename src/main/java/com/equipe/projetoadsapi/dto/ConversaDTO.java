package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.Conversa;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados da Conversa de Chat")
public class ConversaDTO {
    private Long id;
    private Long agendamentoId;
    private Long alunoId;
    private String alunoNome;
    private Long professorId;
    private String professorNome;
    private String assunto;
    private Boolean ativa;
    private Long mensagensNaoLidas;
    private Instant criadaEm;
    private Instant atualizadaEm;

    public static ConversaDTO fromEntity(Conversa conversa, Long mensagensNaoLidas) {
        if (conversa == null) return null;
        var ag = conversa.getAgendamento();
        return ConversaDTO.builder()
                .id(conversa.getId())
                .agendamentoId(ag != null ? ag.getId() : null)
                .alunoId(ag != null && ag.getAluno() != null ? ag.getAluno().getId() : null)
                .alunoNome(ag != null && ag.getAluno() != null ? ag.getAluno().getNome() : null)
                .professorId(ag != null && ag.getProfessor() != null ? ag.getProfessor().getId() : null)
                .professorNome(ag != null && ag.getProfessor() != null ? ag.getProfessor().getNome() : null)
                .assunto(ag != null ? (ag.getAssunto() != null ? ag.getAssunto() : (ag.getSaber() != null ? ag.getSaber().getNome() : "Mentoria")) : null)
                .ativa(conversa.getAtiva())
                .mensagensNaoLidas(mensagensNaoLidas != null ? mensagensNaoLidas : 0L)
                .criadaEm(conversa.getCriadaEm())
                .atualizadaEm(conversa.getAtualizadaEm())
                .build();
    }
}
