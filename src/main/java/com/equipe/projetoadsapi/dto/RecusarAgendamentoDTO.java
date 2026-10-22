package com.equipe.projetoadsapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para recusa formal de solicitação pelo professor.
 * Rastreabilidade: Fase 4 - f4-t6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Justificativa obrigatória para o professor recusar uma solicitação")
public class RecusarAgendamentoDTO {

    @NotBlank(message = "A justificativa de recusa é obrigatória")
    @Size(min = 5, max = 500, message = "A justificativa deve conter entre 5 e 500 caracteres")
    @Schema(description = "Motivo pelo qual o professor não pode atender ao horário solicitado", example = "Horário indisponível devido a reunião de colegiado")
    private String justificativa;
}
