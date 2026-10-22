package com.equipe.projetoadsapi.exception;

/**
 * Exceção lançada quando uma regra de negócio do Sistema Orienta é violada
 * (ex: cancelamento fora do prazo de 24h, agendamento em horário conflitante).
 * Rastreabilidade: Fase 2 (Base de Qualidade e Segurança) - DT-002, RN01..RN10
 */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String message) {
        super(message);
    }
}
