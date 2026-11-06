package com.equipe.projetoadsapi.model.enums;

/**
 * Estados do ciclo de vida de uma solicitação de saque financeiro do professor.
 * Rastreabilidade: Fase 7 - Bloco J (BACK-009)
 */
public enum StatusSaque {
    PENDENTE,
    PROCESSANDO,
    CONCLUIDO,
    RECUSADO
}
