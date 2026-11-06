package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.CategoriaTicket;
import com.equipe.projetoadsapi.model.PrioridadeTicket;
import com.equipe.projetoadsapi.model.StatusTicket;
import com.equipe.projetoadsapi.model.TicketSuporte;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

/**
 * DTO de Representação de Ticket de Atendimento na Central de Suporte.
 * Rastreabilidade: Fase 7 - Bloco K (BACK-015)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Representação de Ticket de Atendimento e Suporte")
public class TicketSuporteDTO {

    @Schema(description = "Identificador único do ticket no banco")
    private Long id;

    @Schema(description = "ID do usuário que abriu o ticket")
    private Long usuarioId;

    @Schema(description = "Nome do usuário que abriu o ticket")
    private String usuarioNome;

    @Schema(description = "E-mail do usuário solicitante")
    private String usuarioEmail;

    @Schema(description = "Protocolo oficial auditável no formato SUP-YYYY-XXXXX", example = "SUP-2026-10492")
    private String protocolo;

    @Schema(description = "Título da solicitação")
    private String titulo;

    @Schema(description = "Assunto do chamado")
    private String assunto;

    @Schema(description = "Descrição detalhada do problema ou dúvida")
    private String descricao;

    @Schema(description = "Categoria do ticket")
    private CategoriaTicket categoria;

    @Schema(description = "Prioridade do chamado")
    private PrioridadeTicket prioridade;

    @Schema(description = "Status do atendimento: ABERTO, EM_ATENDIMENTO, AGUARDANDO_USUARIO, RESOLVIDO, FECHADO")
    private StatusTicket status;

    @Schema(description = "Parecer ou resposta do administrador")
    private String resposta;

    @Schema(description = "Resposta oficial do administrador (alias de resposta)")
    private String respostaAdmin;

    @Schema(description = "Nome do administrador responsável pelo atendimento")
    private String respondidoPorNome;

    @Schema(description = "Data e hora em que o chamado foi respondido")
    private Instant respondidoEm;

    @Schema(description = "Data e hora de abertura do chamado")
    private Instant criadoEm;

    @Schema(description = "Data e hora da última atualização do chamado")
    private Instant atualizadoEm;

    public static TicketSuporteDTO fromEntity(TicketSuporte t) {
        if (t == null) return null;
        String tit = t.getTitulo() != null ? t.getTitulo() : t.getAssunto();
        String resp = t.getRespostaAdmin() != null ? t.getRespostaAdmin() : t.getResposta();

        return TicketSuporteDTO.builder()
                .id(t.getId())
                .usuarioId(t.getUsuario() != null ? t.getUsuario().getId() : null)
                .usuarioNome(t.getUsuario() != null ? t.getUsuario().getNome() : null)
                .usuarioEmail(t.getUsuario() != null ? t.getUsuario().getEmail() : null)
                .protocolo(t.getProtocolo())
                .titulo(tit)
                .assunto(tit)
                .descricao(t.getDescricao())
                .categoria(t.getCategoria())
                .prioridade(t.getPrioridade())
                .status(t.getStatus())
                .resposta(resp)
                .respostaAdmin(resp)
                .respondidoPorNome(t.getRespondidoPor() != null ? t.getRespondidoPor().getNome() : null)
                .respondidoEm(t.getRespondidoEm())
                .criadoEm(t.getCriadoEm())
                .atualizadoEm(t.getAtualizadoEm())
                .build();
    }
}
