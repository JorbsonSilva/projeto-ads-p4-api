package com.equipe.projetoadsapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload para envio de nova mensagem no Chat")
public class EnviarMensagemDTO {

    @NotNull(message = "O ID do remetente é obrigatório")
    private Long remetenteId;

    @NotBlank(message = "O conteúdo da mensagem não pode ser vazio")
    @Size(max = 2000, message = "A mensagem não pode exceder 2000 caracteres")
    private String conteudo;
}
