package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.Denuncia;
import com.equipe.projetoadsapi.model.StatusDenuncia;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

/**
 * Representação de denúncia com protocolo auditável na Ouvidoria.
 * Rastreabilidade: Fase 7 - Bloco K (BACK-016)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Representação de denúncia na Ouvidoria com protocolo e parecer da moderação")
public class DenunciaDTO {

    @Schema(description = "ID único da denúncia no banco")
    private Long id;

    @Schema(description = "Protocolo oficial de ouvidoria no formato DEN-YYYY-XXXXX", example = "DEN-2026-84912")
    private String protocolo;

    @Schema(description = "ID do usuário denunciante")
    private Long denuncianteId;

    @Schema(description = "Nome do usuário denunciante")
    private String denuncianteNome;

    @Schema(description = "ID do usuário denunciado")
    private Long denunciadoId;

    @Schema(description = "Nome do usuário denunciado")
    private String denunciadoNome;

    @Schema(description = "ID do agendamento vinculado")
    private Long agendamentoId;

    @Schema(description = "ID da mensagem de chat vinculada")
    private Long mensagemId;

    @Schema(description = "Motivo da denúncia")
    private String motivo;

    @Schema(description = "Descrição detalhada dos fatos")
    private String descricao;

    @Schema(description = "Status da moderação: PENDENTE, EM_ANALISE, PROCEDENTE, IMPROCEDENTE, ARQUIVADA")
    private StatusDenuncia status;

    @Schema(description = "Parecer técnico da equipe de moderação")
    private String parecerModeracao;

    @Schema(description = "Parecer do moderador (alias de parecerModeracao)")
    private String parecerModerador;

    @Schema(description = "ID do moderador/administrador que analisou a denúncia")
    private Long analisadoPorId;

    @Schema(description = "Nome do moderador que analisou a denúncia")
    private String analisadoPorNome;

    @Schema(description = "Data e hora em que a denúncia foi aberta")
    private Instant criadaEm;

    @Schema(description = "Data e hora da decisão da moderação")
    private Instant analisadaEm;

    @Schema(description = "Data da última atualização do registro")
    private Instant dataAtualizacao;

    public static DenunciaDTO fromEntity(Denuncia d) {
        if (d == null) return null;
        String parecer = d.getParecerModerador() != null ? d.getParecerModerador() : d.getParecerModeracao();

        return DenunciaDTO.builder()
                .id(d.getId())
                .protocolo(d.getProtocolo())
                .denuncianteId(d.getDenunciante() != null ? d.getDenunciante().getId() : null)
                .denuncianteNome(d.getDenunciante() != null ? d.getDenunciante().getNome() : null)
                .denunciadoId(d.getDenunciado() != null ? d.getDenunciado().getId() : null)
                .denunciadoNome(d.getDenunciado() != null ? d.getDenunciado().getNome() : null)
                .agendamentoId(d.getAgendamento() != null ? d.getAgendamento().getId() : null)
                .mensagemId(d.getMensagem() != null ? d.getMensagem().getId() : null)
                .motivo(d.getMotivo())
                .descricao(d.getDescricao())
                .status(d.getStatus())
                .parecerModeracao(parecer)
                .parecerModerador(parecer)
                .analisadoPorId(d.getAnalisadoPor() != null ? d.getAnalisadoPor().getId() : null)
                .analisadoPorNome(d.getAnalisadoPor() != null ? d.getAnalisadoPor().getNome() : null)
                .criadaEm(d.getCriadaEm())
                .analisadaEm(d.getAnalisadaEm())
                .dataAtualizacao(d.getDataAtualizacao() != null ? d.getDataAtualizacao() : d.getCriadaEm())
                .build();
    }
}
