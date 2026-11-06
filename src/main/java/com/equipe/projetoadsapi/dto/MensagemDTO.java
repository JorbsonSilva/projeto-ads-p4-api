package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.Mensagem;
import com.equipe.projetoadsapi.model.PapelUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

/**
 * DTO de Mensagem no Chat Interno com confirmação de leitura e anexos.
 * Rastreabilidade: Fase 7 - Bloco K (BACK-014)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Mensagem enviada ou recebida no Chat interno com status de leitura e anexos")
public class MensagemDTO {

    @Schema(description = "ID único da mensagem")
    private Long id;

    @Schema(description = "ID da conversa à qual a mensagem pertence")
    private Long conversaId;

    @Schema(description = "ID do agendamento vinculado à conversa")
    private Long agendamentoId;

    @Schema(description = "ID do remetente da mensagem")
    private Long remetenteId;

    @Schema(description = "Nome do remetente")
    private String remetenteNome;

    @Schema(description = "Papel do remetente (ALUNO ou PROFESSOR)")
    private String remetentePapel;

    @Schema(description = "URL da foto de perfil do remetente")
    private String remetenteFotoUrl;

    @Schema(description = "Conteúdo textual da mensagem")
    private String conteudo;

    @Schema(description = "Tipo da mensagem (TEXTO, IMAGEM, ARQUIVO, LINK, SISTEMA)")
    private String tipo;

    @Schema(description = "URL do anexo (se houver)")
    private String anexoUrl;

    @Schema(description = "Nome do arquivo anexo (se houver)")
    private String nomeArquivo;

    @Schema(description = "Tamanho formatado do arquivo anexo (ex: 2.4 MB)")
    private String tamanhoArquivo;

    @Schema(description = "Indica se a mensagem já foi lida pelo destinatário")
    private Boolean lida;

    @Schema(description = "Timestamp exato da leitura pelo destinatário (confirmação do duplo tique)")
    private Instant lidaEm;

    @Schema(description = "Data e hora de envio da mensagem")
    private Instant enviadaEm;

    @Schema(description = "Status de confirmação de leitura (duplo tique confirmado)")
    private Boolean confirmadaLeitura;

    public static MensagemDTO fromEntity(Mensagem m) {
        if (m == null) return null;

        Long agendamentoId = null;
        if (m.getConversa() != null && m.getConversa().getAgendamento() != null) {
            agendamentoId = m.getConversa().getAgendamento().getId();
        }

        String papel = null;
        String fotoUrl = null;
        if (m.getRemetente() != null) {
            if (m.getRemetente().getPapel() != null) {
                papel = m.getRemetente().getPapel() == PapelUsuario.ROLE_PROFESSOR ? "PROFESSOR" : "ALUNO";
            }
            fotoUrl = m.getRemetente().getFotoUrl();
        }

        boolean lidaBool = Boolean.TRUE.equals(m.getLida());
        boolean confirmada = lidaBool && m.getLidaEm() != null;

        return MensagemDTO.builder()
                .id(m.getId())
                .conversaId(m.getConversa() != null ? m.getConversa().getId() : null)
                .agendamentoId(agendamentoId)
                .remetenteId(m.getRemetente() != null ? m.getRemetente().getId() : null)
                .remetenteNome(m.getRemetente() != null ? m.getRemetente().getNome() : null)
                .remetentePapel(papel)
                .remetenteFotoUrl(fotoUrl)
                .conteudo(m.getConteudo())
                .tipo(m.getTipo() != null ? m.getTipo() : "TEXTO")
                .anexoUrl(m.getAnexoUrl())
                .nomeArquivo(m.getNomeArquivo())
                .tamanhoArquivo(m.getTamanhoArquivo())
                .lida(lidaBool)
                .lidaEm(m.getLidaEm())
                .enviadaEm(m.getEnviadaEm())
                .confirmadaLeitura(confirmada)
                .build();
    }
}
