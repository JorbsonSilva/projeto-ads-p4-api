package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.CategoriaTicket;
import com.equipe.projetoadsapi.model.PrioridadeTicket;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para abertura de ticket de suporte")
public class CriarTicketDTO {

    @NotBlank(message = "O título do ticket é obrigatório")
    @Size(max = 150, message = "O título não pode ter mais de 150 caracteres")
    private String titulo;

    @NotBlank(message = "A descrição do problema ou dúvida é obrigatória")
    private String descricao;

    @NotNull(message = "A categoria é obrigatória")
    private CategoriaTicket categoria;

    private PrioridadeTicket prioridade;
}
