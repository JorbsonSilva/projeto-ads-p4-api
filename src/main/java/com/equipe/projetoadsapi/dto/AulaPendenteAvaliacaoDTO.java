package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.Agendamento;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aula concluída que ainda pode ser avaliada pelo aluno")
public class AulaPendenteAvaliacaoDTO {

    private Long agendamentoId;
    private String professorNome;
    private String disciplinaNome;
    private String assunto;
    private LocalDate data;
    private LocalTime hora;

    public static AulaPendenteAvaliacaoDTO fromEntity(Agendamento agendamento) {
        return AulaPendenteAvaliacaoDTO.builder()
                .agendamentoId(agendamento.getId())
                .professorNome(agendamento.getProfessor().getNome())
                .disciplinaNome(agendamento.getSaber() != null ? agendamento.getSaber().getNome() : null)
                .assunto(agendamento.getAssunto())
                .data(agendamento.getData())
                .hora(agendamento.getHora())
                .build();
    }
}
