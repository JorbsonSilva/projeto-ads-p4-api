package com.equipe.projetoadsapi.model;

/**
 * Estados do ciclo de vida de uma Denúncia na Moderação/Ouvidoria.
 * Rastreabilidade: Fase 7 - Bloco K (BACK-016)
 */
public enum StatusDenuncia {
    PENDENTE,
    EM_ANALISE,
    PROCEDENTE,
    IMPROCEDENTE,
    ARQUIVADA
}
