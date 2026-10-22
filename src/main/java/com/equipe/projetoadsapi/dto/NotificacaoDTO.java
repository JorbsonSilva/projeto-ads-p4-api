package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.Notificacao;
import com.equipe.projetoadsapi.model.TipoNotificacao;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notificação interna do usuário")
public class NotificacaoDTO {
    private Long id;
    private Long usuarioId;
    private String titulo;
    private String mensagem;
    private TipoNotificacao tipo;
    private Boolean lida;
    private Instant dataHora;
    private Long referenciaId;

    public static NotificacaoDTO fromEntity(Notificacao n) {
        if (n == null) return null;
        return NotificacaoDTO.builder()
                .id(n.getId())
                .usuarioId(n.getUsuario() != null ? n.getUsuario().getId() : null)
                .titulo(n.getTitulo())
                .mensagem(n.getMensagem())
                .tipo(n.getTipo())
                .lida(n.getLida())
                .dataHora(n.getDataHora())
                .referenciaId(n.getReferenciaId())
                .build();
    }
}
