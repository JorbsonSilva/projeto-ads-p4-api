package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.Denuncia;
import com.equipe.projetoadsapi.model.StatusDenuncia;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Representação de denúncia")
public class DenunciaDTO {
    private Long id;
    private Long denuncianteId;
    private String denuncianteNome;
    private Long denunciadoId;
    private String denunciadoNome;
    private Long agendamentoId;
    private String motivo;
    private String descricao;
    private StatusDenuncia status;
    private String parecerModeracao;
    private Instant criadaEm;
    private Instant analisadaEm;

    public static DenunciaDTO fromEntity(Denuncia d) {
        if (d == null) return null;
        return DenunciaDTO.builder()
                .id(d.getId())
                .denuncianteId(d.getDenunciante() != null ? d.getDenunciante().getId() : null)
                .denuncianteNome(d.getDenunciante() != null ? d.getDenunciante().getNome() : null)
                .denunciadoId(d.getDenunciado() != null ? d.getDenunciado().getId() : null)
                .denunciadoNome(d.getDenunciado() != null ? d.getDenunciado().getNome() : null)
                .agendamentoId(d.getAgendamento() != null ? d.getAgendamento().getId() : null)
                .motivo(d.getMotivo())
                .descricao(d.getDescricao())
                .status(d.getStatus())
                .parecerModeracao(d.getParecerModeracao())
                .criadaEm(d.getCriadaEm())
                .analisadaEm(d.getAnalisadaEm())
                .build();
    }
}
