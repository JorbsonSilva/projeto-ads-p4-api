package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.Conversa;
import com.equipe.projetoadsapi.model.Mensagem;
import com.equipe.projetoadsapi.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

/**
 * DTO para representação da Conversa de Chat e Caixa de Entrada Unificada.
 * Rastreabilidade: Fase 7 - Bloco K (BACK-013)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados da Conversa de Chat e Caixa de Entrada")
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

    // =========================================================================
    // CAMPOS DE CAIXA DE ENTRADA UNIFICADA (BACK-013)
    // =========================================================================
    @Schema(description = "ID do outro participante da conversa")
    private Long interlocutorId;

    @Schema(description = "Nome do interlocutor")
    private String nomeInterlocutor;

    @Schema(description = "URL da foto de perfil do interlocutor")
    private String fotoUrlInterlocutor;

    @Schema(description = "Papel do interlocutor na aula (ALUNO ou PROFESSOR)")
    private String papelInterlocutor;

    @Schema(description = "Trecho do conteúdo da última mensagem enviada")
    private String ultimaMensagem;

    @Schema(description = "Data e hora de envio da última mensagem")
    private Instant dataHoraUltimaMensagem;

    @Schema(description = "Indica se a última mensagem foi enviada pelo usuário logado")
    private Boolean enviadaPorMim;

    @Schema(description = "Nome da disciplina ou assunto da mentoria")
    private String disciplinaNome;

    /**
     * Mapeador legado compatível.
     */
    public static ConversaDTO fromEntity(Conversa conversa, Long mensagensNaoLidas) {
        if (conversa == null) return null;
        var ag = conversa.getAgendamento();
        String assuntoOuSaber = ag != null ? (ag.getAssunto() != null ? ag.getAssunto() : (ag.getSaber() != null ? ag.getSaber().getNome() : "Mentoria")) : null;

        return ConversaDTO.builder()
                .id(conversa.getId())
                .agendamentoId(ag != null ? ag.getId() : null)
                .alunoId(ag != null && ag.getAluno() != null ? ag.getAluno().getId() : null)
                .alunoNome(ag != null && ag.getAluno() != null ? ag.getAluno().getNome() : null)
                .professorId(ag != null && ag.getProfessor() != null ? ag.getProfessor().getId() : null)
                .professorNome(ag != null && ag.getProfessor() != null ? ag.getProfessor().getNome() : null)
                .assunto(assuntoOuSaber)
                .disciplinaNome(assuntoOuSaber)
                .ativa(conversa.getAtiva())
                .mensagensNaoLidas(mensagensNaoLidas != null ? mensagensNaoLidas : 0L)
                .criadaEm(conversa.getCriadaEm())
                .atualizadaEm(conversa.getAtualizadaEm())
                .build();
    }

    /**
     * Mapeador enriquecido contextualizado ao usuário autenticado (BACK-013).
     */
    public static ConversaDTO fromEntity(Conversa conversa, Long usuarioAutenticadoId, Long mensagensNaoLidas, Mensagem ultimaMensagem) {
        if (conversa == null) return null;
        var ag = conversa.getAgendamento();
        String assuntoOuSaber = ag != null ? (ag.getAssunto() != null ? ag.getAssunto() : (ag.getSaber() != null ? ag.getSaber().getNome() : "Mentoria")) : null;

        Usuario aluno = ag != null ? ag.getAluno() : null;
        Usuario professor = ag != null ? ag.getProfessor() : null;

        Long interlocutorId = null;
        String nomeInterlocutor = null;
        String fotoUrlInterlocutor = null;
        String papelInterlocutor = null;

        if (usuarioAutenticadoId != null && aluno != null && usuarioAutenticadoId.equals(aluno.getId())) {
            // O usuário autenticado é o ALUNO -> Interlocutor é o PROFESSOR
            if (professor != null) {
                interlocutorId = professor.getId();
                nomeInterlocutor = professor.getNome();
                fotoUrlInterlocutor = professor.getFotoUrl();
                papelInterlocutor = "PROFESSOR";
            }
        } else if (usuarioAutenticadoId != null && professor != null && usuarioAutenticadoId.equals(professor.getId())) {
            // O usuário autenticado é o PROFESSOR -> Interlocutor é o ALUNO
            if (aluno != null) {
                interlocutorId = aluno.getId();
                nomeInterlocutor = aluno.getNome();
                fotoUrlInterlocutor = aluno.getFotoUrl();
                papelInterlocutor = "ALUNO";
            }
        } else {
            // Fallback genérico caso usuarioAutenticadoId não corresponda a nenhum dos dois
            if (professor != null) {
                interlocutorId = professor.getId();
                nomeInterlocutor = professor.getNome();
                fotoUrlInterlocutor = professor.getFotoUrl();
                papelInterlocutor = "PROFESSOR";
            } else if (aluno != null) {
                interlocutorId = aluno.getId();
                nomeInterlocutor = aluno.getNome();
                fotoUrlInterlocutor = aluno.getFotoUrl();
                papelInterlocutor = "ALUNO";
            }
        }

        String textoUltimaMensagem = null;
        Instant dataHoraUltimaMensagem = conversa.getAtualizadaEm() != null ? conversa.getAtualizadaEm() : conversa.getCriadaEm();
        Boolean enviadaPorMim = false;

        if (ultimaMensagem != null) {
            textoUltimaMensagem = ultimaMensagem.getConteudo();
            if (ultimaMensagem.getEnviadaEm() != null) {
                dataHoraUltimaMensagem = ultimaMensagem.getEnviadaEm();
            }
            if (usuarioAutenticadoId != null && ultimaMensagem.getRemetente() != null) {
                enviadaPorMim = usuarioAutenticadoId.equals(ultimaMensagem.getRemetente().getId());
            }
        }

        return ConversaDTO.builder()
                .id(conversa.getId())
                .agendamentoId(ag != null ? ag.getId() : null)
                .alunoId(aluno != null ? aluno.getId() : null)
                .alunoNome(aluno != null ? aluno.getNome() : null)
                .professorId(professor != null ? professor.getId() : null)
                .professorNome(professor != null ? professor.getNome() : null)
                .assunto(assuntoOuSaber)
                .disciplinaNome(assuntoOuSaber)
                .ativa(conversa.getAtiva())
                .mensagensNaoLidas(mensagensNaoLidas != null ? mensagensNaoLidas : 0L)
                .criadaEm(conversa.getCriadaEm())
                .atualizadaEm(conversa.getAtualizadaEm())
                .interlocutorId(interlocutorId)
                .nomeInterlocutor(nomeInterlocutor)
                .fotoUrlInterlocutor(fotoUrlInterlocutor)
                .papelInterlocutor(papelInterlocutor)
                .ultimaMensagem(textoUltimaMensagem)
                .dataHoraUltimaMensagem(dataHoraUltimaMensagem)
                .enviadaPorMim(enviadaPorMim)
                .build();
    }
}
