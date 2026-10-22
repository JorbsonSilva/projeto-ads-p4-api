package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.StatusTicket;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta do suporte ao ticket")
public class ResponderTicketDTO {

    @NotBlank(message = "A resposta do atendimento é obrigatória")
    private String resposta;

    private StatusTicket novoStatus;
}
