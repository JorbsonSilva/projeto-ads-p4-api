package com.equipe.projetoadsapi.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Dados para envio de denúncia de usuário ou conduta na Ouvidoria.
 * Rastreabilidade: Fase 7 - Bloco K (BACK-016)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para envio de denúncia de usuário ou conduta na Ouvidoria")
public class CriarDenunciaDTO {

    @Schema(description = "ID do usuário denunciado (opcional se vinculado a agendamento)", example = "20")
    private Long denunciadoId;

    @Schema(description = "ID do agendamento vinculado à ocorrência", example = "101")
    private Long agendamentoId;

    @Schema(description = "ID da mensagem específica do chat vinculada à ocorrência", example = "501")
    private Long mensagemId;

    @NotBlank(message = "O motivo da denúncia é obrigatório")
    @Size(max = 100, message = "O motivo deve ter no máximo 100 caracteres")
    @Schema(description = "Motivo resumido da denúncia: Assédio, Ausência sem aviso, Linguagem imprópria, etc.", example = "Conduta Inadequada")
    private String motivo;

    @JsonAlias({"detalhes", "texto", "relato", "mensagem"})
    @NotBlank(message = "A descrição detalhada da conduta é obrigatória")
    @Size(max = 4000, message = "A descrição deve ter no máximo 4000 caracteres")
    @Schema(description = "Descrição detalhada dos fatos ocorridos", example = "O usuário utilizou linguagem ofensiva durante a sessão.")
    private String descricao;
}
