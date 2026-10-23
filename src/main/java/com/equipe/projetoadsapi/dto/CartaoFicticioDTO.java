package com.equipe.projetoadsapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para cadastro de dados de pagamento fictício/demonstrativo (PROP-FIN-01 / Decisão Técnica 18).
 * 
 * Regras de Segurança e Conformidade da Baseline 2.0.1:
 * - Apenas a bandeira e os 4 últimos dígitos são salvos para exibição na UI.
 * - O código de segurança (CVV) é sumariamente ignorado e NUNCA é persistido em banco.
 * - Não há cobrança nem transação monetária real.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para cadastro de cartão fictício para fins de demonstração acadêmica (sem cobrança real)")
public class CartaoFicticioDTO {

    @NotBlank(message = "O nome impresso no cartão é obrigatório")
    @Size(min = 3, max = 100, message = "O nome do titular deve ter entre 3 e 100 caracteres")
    @Schema(description = "Nome do titular do cartão", example = "LUCAS G SILVA")
    private String titular;

    @NotBlank(message = "O número do cartão (completo ou mascarado) é obrigatório")
    @Schema(description = "Número do cartão fictício (apenas os 4 últimos dígitos são persistidos)", example = "•••• •••• •••• 4242")
    private String numeroCartao;

    @Schema(description = "Bandeira do cartão (ex: Mastercard, Visa, Elo)", example = "Mastercard")
    private String bandeira;

    @Schema(description = "Mês e ano de validade para fins de exibição", example = "12/28")
    private String validade;

    /**
     * Extrai com segurança os 4 últimos dígitos numéricos de uma string
     */
    public String extrairUltimosQuatroDigitos() {
        if (numeroCartao == null) return "4242";
        String apenasDigitos = numeroCartao.replaceAll("\\D", "");
        if (apenasDigitos.length() >= 4) {
            return apenasDigitos.substring(apenasDigitos.length() - 4);
        }
        return apenasDigitos.isEmpty() ? "4242" : apenasDigitos;
    }

    /**
     * Detecta ou padroniza a bandeira do cartão fictício
     */
    public String resolverBandeira() {
        if (bandeira != null && !bandeira.isBlank()) {
            return bandeira.trim();
        }
        if (numeroCartao != null) {
            if (numeroCartao.startsWith("4")) return "Visa";
            if (numeroCartao.startsWith("5")) return "Mastercard";
            if (numeroCartao.startsWith("6")) return "Elo";
        }
        return "Mastercard";
    }
}
