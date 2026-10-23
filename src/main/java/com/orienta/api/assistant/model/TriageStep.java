package com.orienta.api.assistant.model;

/**
 * Etapas da máquina de estados finitos (FSM) da triagem determinística do Orienta.
 * Cada etapa possui transições rígidas e auditáveis sem alucinação de IA.
 */
public enum TriageStep {
    INITIAL,              // Boas-vindas e introdução das regras do assistente
    SUBJECT_SELECTION,    // Escolha da Disciplina cadastrada no catálogo
    TOPIC_SELECTION,      // Escolha do Assunto/Tópico específico da disciplina
    MODALITY_BUDGET,      // Escolha de Modalidade (ONLINE/PRESENCIAL) e Teto Orçamentário
    RECOMMENDATION,       // Exibição dos professores ativos compatíveis (apenas consulta)
    SUPPORT_HANDOFF,      // Redirecionamento seguro para fila de suporte humano (tb_suporte_tickets)
    COMPLETED             // Triagem finalizada com card de ação manual gerado
}
