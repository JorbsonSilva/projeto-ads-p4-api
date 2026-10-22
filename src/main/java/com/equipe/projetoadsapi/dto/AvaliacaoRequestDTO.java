package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.Avaliacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTOs de solicitação e resposta de avaliação pós-aula.
 * Rastreabilidade: Fase 6 - f6-t8
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para envio de avaliação e feedback pós-aula")
public class AvaliacaoRequestDTO {

    @NotNull(message = "A nota geral é obrigatória")
    @Min(value = 1, message = "A nota mínima é 1")
    @Max(value = 5, message = "A nota máxima é 5")
    @Schema(description = "Nota geral de 1 a 5 estrelas", example = "5")
    private Integer nota;

    @Min(value = 1, message = "A pontualidade mínima é 1")
    @Max(value = 5, message = "A pontualidade máxima é 5")
    @Schema(description = "Avaliação de pontualidade de 1 a 5", example = "5")
    private Integer pontualidade;

    @Min(value = 1, message = "A didática mínima é 1")
    @Max(value = 5, message = "A didática máxima é 5")
    @Schema(description = "Avaliação de didática ou clareza de 1 a 5", example = "5")
    private Integer didatica;

    @Schema(description = "Comentário e depoimento sobre a aula", example = "Excelente mentoria! Explicou árvores AVL com muita clareza.")
    private String comentario;
}
