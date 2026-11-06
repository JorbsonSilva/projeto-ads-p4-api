package com.equipe.projetoadsapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Payload para envio de mensagem no Chat contextual à aula.
 * Rastreabilidade: Fase 7 - Bloco K (BACK-014)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload para envio de nova mensagem no Chat")
public class EnviarMensagemDTO {

    @NotNull(message = "O ID do remetente é obrigatório")
    @Schema(description = "ID do remetente da mensagem", example = "10")
    private Long remetenteId;

    @NotBlank(message = "O conteúdo da mensagem não pode ser vazio")
    @Size(max = 2000, message = "A mensagem não pode exceder 2000 caracteres")
    @Schema(description = "Texto da mensagem", example = "Olá, nos vemos às 14h!")
    private String conteudo;

    @Schema(description = "Tipo da mensagem: TEXTO, IMAGEM, ARQUIVO, LINK", example = "TEXTO")
    private String tipo;

    @Schema(description = "URL do anexo (se houver)", example = "https://orienta.com.br/anexos/doc.pdf")
    private String anexoUrl;

    @Schema(description = "Nome do arquivo anexo", example = "roteiro.pdf")
    private String nomeArquivo;

    @Schema(description = "Tamanho formatado do arquivo anexo", example = "2.5 MB")
    private String tamanhoArquivo;

    @Schema(description = "ID do agendamento (opcional se fornecido na URL)")
    private Long agendamentoId;

    @Schema(description = "ID da conversa (opcional se fornecido na URL)")
    private Long conversaId;
}
