package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.Denuncia;
import com.equipe.projetoadsapi.model.StatusDenuncia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para envio de denúncia de usuário ou conduta")
public class CriarDenunciaDTO {

    @NotNull(message = "O ID do denunciado é obrigatório")
    private Long denunciadoId;

    private Long agendamentoId;

    @NotBlank(message = "O motivo da denúncia é obrigatório")
    @Size(max = 100, message = "O motivo deve ter no máximo 100 caracteres")
    private String motivo;

    @NotBlank(message = "A descrição detalhada da conduta é obrigatória")
    private String descricao;
}
