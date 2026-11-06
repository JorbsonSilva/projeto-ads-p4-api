package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.TransacaoFinanceira;
import com.equipe.projetoadsapi.model.enums.TipoTransacao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO para exibição de lançamentos contábeis no extrato financeiro.
 * Rastreabilidade: Fase 7 - Bloco J (BACK-009)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransacaoFinanceiraDTO {

    private Long id;
    private Long usuarioId;
    private Long agendamentoId;
    private Long saqueId;
    private TipoTransacao tipoTransacao;
    private BigDecimal valor;
    private String descricao;
    private Instant dataTransacao;

    public static TransacaoFinanceiraDTO fromEntity(TransacaoFinanceira tf) {
        if (tf == null) return null;
        return TransacaoFinanceiraDTO.builder()
                .id(tf.getId())
                .usuarioId(tf.getUsuario() != null ? tf.getUsuario().getId() : null)
                .agendamentoId(tf.getAgendamento() != null ? tf.getAgendamento().getId() : null)
                .saqueId(tf.getSaque() != null ? tf.getSaque().getId() : null)
                .tipoTransacao(tf.getTipoTransacao())
                .valor(tf.getValor())
                .descricao(tf.getDescricao())
                .dataTransacao(tf.getDataTransacao())
                .build();
    }
}
