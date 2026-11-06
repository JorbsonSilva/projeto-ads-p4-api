package com.equipe.projetoadsapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO para requisição de envio de mensagem unificada no Chat.
 * Suporta identificação flexível por agendamentoId ou conversaId,
 * além de anexos e tipos diversos de mensagem.
 * Rastreabilidade: Fase 7 - Bloco K (BACK-014)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload para envio unificado de mensagem no Chat")
public class MensagemRequestDTO {

    @Schema(description = "ID do agendamento vinculado à conversa (opcional se conversaId for fornecido)")
    private Long agendamentoId;

    @Schema(description = "ID direto da conversa (opcional se agendamentoId for fornecido)")
    private Long conversaId;

    @Schema(description = "ID do remetente (opcional, defaults para o usuário autenticado)")
    private Long remetenteId;

    @NotBlank(message = "O conteúdo da mensagem não pode ser vazio")
    @Size(max = 2000, message = "A mensagem não pode exceder 2000 caracteres")
    @Schema(description = "Conteúdo textual da mensagem", example = "Olá professor, estou na sala!")
    private String conteudo;

    @Schema(description = "Tipo da mensagem: TEXTO, IMAGEM, ARQUIVO, LINK", example = "TEXTO")
    private String tipo;

    @Schema(description = "URL do anexo (se houver)", example = "https://orienta.com.br/anexos/exercicios.pdf")
    private String anexoUrl;

    @Schema(description = "Nome do arquivo anexo", example = "exercicios_matematica.pdf")
    private String nomeArquivo;

    @Schema(description = "Tamanho do arquivo anexo", example = "1.2 MB")
    private String tamanhoArquivo;
}
