package com.equipe.projetoadsapi.model;

/**
 * Tipos de eventos registrados durante a execução de uma aula.
 * Rastreabilidade: Fase 6 - f6-t4, f6-t5
 */
public enum TipoEventoAula {
    CHECKIN_ALUNO,
    CHECKIN_PROFESSOR,
    INICIO_AULA,
    PAUSA,
    RETOMADA,
    SOLICITACAO_FIM,
    CONFIRMACAO_FIM
}
