package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.StatusTicket;
import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Payload para resposta e parecer do atendimento administrativo ao ticket.
 * Rastreabilidade: Fase 7 - Bloco K (BACK-015)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta do suporte ao ticket de atendimento")
public class ResponderTicketDTO {

    @JsonAlias({"respostaAdmin", "parecer", "mensagem"})
    @NotBlank(message = "A resposta do atendimento é obrigatória")
    @Schema(description = "Texto da resposta ou parecer técnico do suporte", example = "Sua solicitação foi analisada e o estorno processado com sucesso.")
    private String resposta;

    @Schema(description = "Novo status atribuído ao ticket: RESOLVIDO, EM_ATENDIMENTO, AGUARDANDO_USUARIO, FECHADO", example = "RESOLVIDO")
    private StatusTicket novoStatus;
}
