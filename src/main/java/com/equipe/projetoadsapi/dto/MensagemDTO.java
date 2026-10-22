package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.Mensagem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Mensagem enviada ou recebida no Chat")
public class MensagemDTO {
    private Long id;
    private Long conversaId;
    private Long remetenteId;
    private String remetenteNome;
    private String conteudo;
    private Boolean lida;
    private Instant lidaEm;
    private Instant enviadaEm;

    public static MensagemDTO fromEntity(Mensagem m) {
        if (m == null) return null;
        return MensagemDTO.builder()
                .id(m.getId())
                .conversaId(m.getConversa() != null ? m.getConversa().getId() : null)
                .remetenteId(m.getRemetente() != null ? m.getRemetente().getId() : null)
                .remetenteNome(m.getRemetente() != null ? m.getRemetente().getNome() : null)
                .conteudo(m.getConteudo())
                .lida(m.getLida())
                .lidaEm(m.getLidaEm())
                .enviadaEm(m.getEnviadaEm())
                .build();
    }
}
