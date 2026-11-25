package com.equipe.projetoadsapi.tutor.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class RagInternalDtos {

    private RagInternalDtos() {
    }

    public enum Mode {
        @JsonProperty("demonstrative") DEMONSTRATIVE,
        @JsonProperty("simulated") SIMULATED,
        @JsonProperty("semantic_real") SEMANTIC_REAL
    }

    public enum Outcome {
        @JsonProperty("answered") ANSWERED,
        @JsonProperty("no_evidence") NO_EVIDENCE
    }

    public enum HealthStatus {
        @JsonProperty("alive") ALIVE,
        @JsonProperty("ready") READY
    }

    public record Conversation(UUID conversationId, Mode mode, OffsetDateTime expiresAt) {
    }

    record Question(UUID messageId, String question) {
    }

    public record Evidence(String id, String corpus, int corpusVersion) {
    }

    public record Answer(UUID conversationId, UUID messageId, Mode mode, Outcome outcome,
                         String answer, List<Evidence> evidence) {
        public Answer {
            evidence = evidence == null ? null : List.copyOf(evidence);
        }
    }

    public record Liveness(HealthStatus status) {
    }

    public record Readiness(HealthStatus status, Mode mode) {
    }

    record ErrorEnvelope(ErrorBody error) {
    }

    record ErrorBody(String code, String message, boolean retryable) {
    }
}
