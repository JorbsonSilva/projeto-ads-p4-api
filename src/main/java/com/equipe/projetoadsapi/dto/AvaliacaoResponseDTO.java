package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.Avaliacao;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO de resposta para avaliação registrada.
 * Rastreabilidade: Fase 6 - f6-t8
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados da avaliação registrada")
public class AvaliacaoResponseDTO {

    private Long id;
    private Long agendamentoId;
    private Long avaliadorId;
    private String avaliadorNome;
    private Long avaliadoId;
    private String avaliadoNome;
    private Integer nota;
    private Integer pontualidade;
    private Integer didatica;
    private String comentario;
    private Instant criadaEm;

    public static AvaliacaoResponseDTO fromEntity(Avaliacao a) {
        if (a == null) return null;

        return AvaliacaoResponseDTO.builder()
                .id(a.getId())
                .agendamentoId(a.getAgendamento() != null ? a.getAgendamento().getId() : null)
                .avaliadorId(a.getAvaliador() != null ? a.getAvaliador().getId() : null)
                .avaliadorNome(a.getAvaliador() != null ? a.getAvaliador().getNome() : null)
                .avaliadoId(a.getAvaliado() != null ? a.getAvaliado().getId() : null)
                .avaliadoNome(a.getAvaliado() != null ? a.getAvaliado().getNome() : null)
                .nota(a.getNota())
                .pontualidade(a.getPontualidade())
                .didatica(a.getDidatica())
                .comentario(a.getComentario())
                .criadaEm(a.getCriadaEm())
                .build();
    }
}
