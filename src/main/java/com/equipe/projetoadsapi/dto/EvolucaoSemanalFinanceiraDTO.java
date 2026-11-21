package com.equipe.projetoadsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvolucaoSemanalFinanceiraDTO {

    private String label;
    private BigDecimal valor;
    private Integer aulas;
}
