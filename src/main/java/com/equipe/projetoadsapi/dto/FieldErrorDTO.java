package com.equipe.projetoadsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Detalhe de validação de campo individual para Bean Validation.
 * Rastreabilidade: Fase 2 (Base de Qualidade e Segurança) - DT-002
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldErrorDTO {
    private String campo;
    private String mensagem;
}
