package com.equipe.projetoadsapi.model;

/**
 * Enum padronizado de status de agendamento de aulas.
 * Rastreabilidade: Fase 4 (Padronizar Contrato Mobile/API) - f4-t7
 * Alinhado estritamente com os tipos TypeScript do aplicativo mobile.
 */
public enum StatusAgendamento {
    PENDENTE("Solicitação aguardando resposta do professor"),
    CONFIRMADO("Aula confirmada pelo professor"),
    RECUSADO("Solicitação recusada pelo professor com justificativa"),
    CANCELADO("Agendamento cancelado por uma das partes"),
    EM_ANDAMENTO("Aula iniciada e cronômetro ativo no servidor"),
    AGUARDANDO_CONFIRMACAO("Aula encerrada, aguardando confirmação mútua"),
    REALIZADA("Aula concluída e confirmada por ambas as partes");

    private final String descricao;

    StatusAgendamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
