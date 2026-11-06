package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.CategoriaTicket;
import com.equipe.projetoadsapi.model.PrioridadeTicket;
import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO para solicitação de abertura de ticket de suporte.
 * Rastreabilidade: Fase 7 - Bloco K (BACK-015)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para abertura de ticket de atendimento na Central de Suporte")
public class CriarTicketDTO {

    @JsonAlias({"assunto", "subject"})
    @Size(max = 150, message = "O título não pode ter mais de 150 caracteres")
    @Schema(description = "Título ou assunto resumido da solicitação", example = "Dúvida sobre confirmação de mentoria")
    private String titulo;

    @JsonAlias({"titulo", "subject"})
    @Size(max = 150, message = "O assunto não pode ter mais de 150 caracteres")
    @Schema(description = "Assunto do chamado (alias para titulo)", example = "Dúvida sobre confirmação de mentoria")
    private String assunto;

    @NotBlank(message = "A descrição do problema ou dúvida é obrigatória")
    @Size(max = 4000, message = "A descrição não pode ter mais de 4000 caracteres")
    @Schema(description = "Descrição detalhada do chamado", example = "Gostaria de saber como funciona o estorno em caso de cancelamento.")
    private String descricao;

    @NotNull(message = "A categoria é obrigatória")
    @Schema(description = "Categoria do ticket: DUVIDA, PAGAMENTO, FINANCEIRO, TECNICO, PROBLEMA_TECNICO, CONDUTA, OUTRO", example = "DUVIDA")
    private CategoriaTicket categoria;

    @Schema(description = "Prioridade do chamado: BAIXA, MEDIA, ALTA, URGENTE", example = "MEDIA")
    private PrioridadeTicket prioridade;

    public String getTitulo() {
        if (titulo != null && !titulo.isBlank()) return titulo;
        return assunto != null && !assunto.isBlank() ? assunto : "Chamado de Suporte";
    }

    public String getAssunto() {
        if (assunto != null && !assunto.isBlank()) return assunto;
        return titulo != null && !titulo.isBlank() ? titulo : "Chamado de Suporte";
    }
}
