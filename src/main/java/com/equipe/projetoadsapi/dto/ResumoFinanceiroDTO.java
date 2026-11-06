package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.enums.TipoChavePix;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO que consolida a posição financeira do professor para a Tela de Saque e Painel de Ganhos.
 * Rastreabilidade: Fase 7 - Bloco J (BACK-009)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumoFinanceiroDTO {

    @Builder.Default
    private BigDecimal saldoDisponivel = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal saldoPendente = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalGanhosBrutos = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalTaxasRetidas = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalSacado = BigDecimal.ZERO;

    private String chavePixCadastrada;

    private TipoChavePix tipoChavePixCadastrada;

    @Builder.Default
    private Boolean saqueBloqueado = false;

    private String motivoBloqueio;

    @Builder.Default
    private List<TransacaoFinanceiraDTO> transacoesRecentes = new ArrayList<>();

    @Builder.Default
    private List<SaqueResponseDTO> saquesRecentes = new ArrayList<>();
}
