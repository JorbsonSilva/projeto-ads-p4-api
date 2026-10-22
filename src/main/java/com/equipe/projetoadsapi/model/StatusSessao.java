package com.equipe.projetoadsapi.model;

/**
 * Estados da Sessão de Aula em Tempo Real.
 * Rastreabilidade: Fase 6 - f6-t1, f6-t2, f6-t6
 */
public enum StatusSessao {
    AGUARDANDO_CHECKIN("Aguardando ambos aluno e professor realizarem o check-in"),
    EM_ANDAMENTO("Aula iniciada com cronômetro ativo no servidor"),
    PAUSADA("Aula temporariamente pausada por comum acordo"),
    AGUARDANDO_CONFIRMACAO("Uma das partes encerrou, aguardando aceite mútuo"),
    CONCLUIDA("Aula finalizada com aceite mútuo de conclusão");

    private final String descricao;

    StatusSessao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
