package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.Saque;
import com.equipe.projetoadsapi.model.enums.StatusSaque;
import com.equipe.projetoadsapi.model.enums.TipoChavePix;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO de resposta detalhando uma solicitação de saque.
 * Rastreabilidade: Fase 7 - Bloco J (BACK-009)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaqueResponseDTO {

    private Long id;
    private Long professorId;
    private String nomeProfessor;
    private BigDecimal valor;
    private BigDecimal taxaAplicada;
    private BigDecimal valorLiquido;
    private StatusSaque statusSaque;
    private TipoChavePix tipoChavePix;
    private String chavePix;
    private String dadosBancarios;
    private String protocoloSaque;
    private Instant dataSolicitacao;
    private Instant dataProcessamento;
    private String observacoes;

    public static SaqueResponseDTO fromEntity(Saque saque) {
        if (saque == null) return null;
        return SaqueResponseDTO.builder()
                .id(saque.getId())
                .professorId(saque.getProfessor() != null ? saque.getProfessor().getId() : null)
                .nomeProfessor(saque.getProfessor() != null ? saque.getProfessor().getNome() : null)
                .valor(saque.getValor())
                .taxaAplicada(saque.getTaxaAplicada())
                .valorLiquido(saque.getValorLiquido())
                .statusSaque(saque.getStatusSaque())
                .tipoChavePix(saque.getTipoChavePix())
                .chavePix(saque.getChavePix())
                .dadosBancarios(saque.getDadosBancarios())
                .protocoloSaque(saque.getProtocoloSaque())
                .dataSolicitacao(saque.getDataSolicitacao())
                .dataProcessamento(saque.getDataProcessamento())
                .observacoes(saque.getObservacoes())
                .build();
    }

    public String getProtocolo() {
        return protocoloSaque;
    }

    public StatusSaque getStatus() {
        return statusSaque;
    }
}
