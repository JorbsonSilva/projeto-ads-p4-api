package com.equipe.projetoadsapi.exception;

/**
 * Exceção lançada quando uma entidade solicitada não existe no banco de dados (HTTP 404).
 * Rastreabilidade: Fase 2 (Base de Qualidade e Segurança) - DT-002
 */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String message) {
        super(message);
    }
}
