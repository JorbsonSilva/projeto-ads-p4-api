package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.enums.TipoChavePix;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para solicitação de saque pelo professor.
 * Rastreabilidade: Fase 7 - Bloco J (BACK-009)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaqueRequestDTO {

    @NotNull(message = "O valor do saque é obrigatório")
    @DecimalMin(value = "10.00", message = "O valor mínimo para solicitação de saque é de R$ 10,00")
    private BigDecimal valor;

    @NotNull(message = "O tipo de chave PIX é obrigatório")
    private TipoChavePix tipoChavePix;

    @NotBlank(message = "A chave PIX é obrigatória")
    private String chavePix;

    private String dadosBancarios;

    private String observacoes;
}
