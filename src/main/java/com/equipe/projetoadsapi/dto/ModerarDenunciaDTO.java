package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.StatusDenuncia;
import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Payload para parecer de moderação e julgamento de denúncia (exclusivo para ADMIN).
 * Rastreabilidade: Fase 7 - Bloco K (BACK-016)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Parecer de moderação e julgamento de denúncia na Ouvidoria")
public class ModerarDenunciaDTO {

    @JsonAlias({"parecer", "parecerModeracao", "justificativa", "mensagem"})
    @NotBlank(message = "O parecer da moderação é obrigatório")
    @Schema(description = "Parecer técnico da moderação", example = "Denúncia analisada e julgada procedente após conferência do log de mensagens.")
    private String parecerModerador;

    @NotNull(message = "O novo status da denúncia é obrigatório")
    @Schema(description = "Novo status da denúncia: PROCEDENTE, IMPROCEDENTE, ARQUIVADA, EM_ANALISE", example = "PROCEDENTE")
    private StatusDenuncia novoStatus;
}
