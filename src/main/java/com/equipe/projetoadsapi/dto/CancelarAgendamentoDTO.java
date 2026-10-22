package com.equipe.projetoadsapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para cancelamento formal de agendamento.
 * Rastreabilidade: Fase 4 - f4-t6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Justificativa obrigatória para cancelamento de agendamento")
public class CancelarAgendamentoDTO {

    @NotBlank(message = "A justificativa de cancelamento é obrigatória")
    @Size(min = 5, max = 500, message = "A justificativa deve conter entre 5 e 500 caracteres")
    @Schema(description = "Motivo detalhado do cancelamento da aula", example = "Imprevisto de saúde impossibilitando comparecimento")
    private String justificativa;
}
