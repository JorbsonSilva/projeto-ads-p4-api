package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.CategoriaTicket;
import com.equipe.projetoadsapi.model.PrioridadeTicket;
import com.equipe.projetoadsapi.model.StatusTicket;
import com.equipe.projetoadsapi.model.TicketSuporte;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Representação de Ticket de Atendimento")
public class TicketSuporteDTO {
    private Long id;
    private Long usuarioId;
    private String usuarioNome;
    private String usuarioEmail;
    private String protocolo;
    private String titulo;
    private String descricao;
    private CategoriaTicket categoria;
    private PrioridadeTicket prioridade;
    private StatusTicket status;
    private String resposta;
    private String respondidoPorNome;
    private Instant respondidoEm;
    private Instant criadoEm;
    private Instant atualizadoEm;

    public static TicketSuporteDTO fromEntity(TicketSuporte t) {
        if (t == null) return null;
        return TicketSuporteDTO.builder()
                .id(t.getId())
                .usuarioId(t.getUsuario() != null ? t.getUsuario().getId() : null)
                .usuarioNome(t.getUsuario() != null ? t.getUsuario().getNome() : null)
                .usuarioEmail(t.getUsuario() != null ? t.getUsuario().getEmail() : null)
                .protocolo(t.getProtocolo())
                .titulo(t.getTitulo())
                .descricao(t.getDescricao())
                .categoria(t.getCategoria())
                .prioridade(t.getPrioridade())
                .status(t.getStatus())
                .resposta(t.getResposta())
                .respondidoPorNome(t.getRespondidoPor() != null ? t.getRespondidoPor().getNome() : null)
                .respondidoEm(t.getRespondidoEm())
                .criadoEm(t.getCriadoEm())
                .atualizadoEm(t.getAtualizadoEm())
                .build();
    }
}
