package com.equipe.projetoadsapi.tutor.client;

public final class RagClientException extends RuntimeException {

    public enum Code {
        INTERNAL_AUTHENTICATION,
        CONVERSATION_NOT_FOUND,
        MESSAGE_CONFLICT,
        TURN_IN_PROGRESS,
        CONTRACT_REJECTED,
        SESSION_LIMIT,
        MESSAGE_LIMIT,
        CONCURRENCY_LIMIT,
        MODEL_UNAVAILABLE,
        INDEX_UNAVAILABLE,
        INDEX_INCOMPATIBLE,
        PROVIDER_UNAVAILABLE,
        PROVIDER_TIMEOUT,
        CONNECTION_FAILED,
        CONNECTION_TIMEOUT,
        RESPONSE_TIMEOUT,
        INVALID_RESPONSE,
        UNEXPECTED_STATUS
    }

    private final Code code;
    private final boolean retryable;

    RagClientException(Code code, boolean retryable) {
        super("Falha na comunicação com o tutor. Código: " + code.name());
        this.code = code;
        this.retryable = retryable;
    }

    public Code code() {
        return code;
    }

    public boolean retryable() {
        return retryable;
    }
}
