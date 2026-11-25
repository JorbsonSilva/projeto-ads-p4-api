package com.equipe.projetoadsapi.tutor.client;

import com.equipe.projetoadsapi.tutor.dto.TutorMensagemRequestDTO;
import com.equipe.projetoadsapi.tutor.security.RagInternalTokenIssuer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import com.equipe.projetoadsapi.tutor.client.RagClientException.Code;
import com.equipe.projetoadsapi.tutor.client.RagInternalDtos.Answer;
import com.equipe.projetoadsapi.tutor.client.RagInternalDtos.Conversation;
import com.equipe.projetoadsapi.tutor.client.RagInternalDtos.ErrorEnvelope;
import com.equipe.projetoadsapi.tutor.client.RagInternalDtos.Evidence;
import com.equipe.projetoadsapi.tutor.client.RagInternalDtos.HealthStatus;
import com.equipe.projetoadsapi.tutor.client.RagInternalDtos.Liveness;
import com.equipe.projetoadsapi.tutor.client.RagInternalDtos.Outcome;
import com.equipe.projetoadsapi.tutor.client.RagInternalDtos.Question;
import com.equipe.projetoadsapi.tutor.client.RagInternalDtos.Readiness;

public final class RagHttpClient {

    private static final String CONVERSATIONS = "/internal/v1/conversations";
    private static final String CONVERSATION = CONVERSATIONS + "/{conversationId}";
    private static final String HEALTH = "/internal/v1/health/";
    private static final int MAX_RESPONSE_BYTES = 64 * 1024;

    private final RestClient http;
    private final RagInternalTokenIssuer issuer;
    private final ObjectReader reader;
    private final Semaphore capacity;

    RagHttpClient(RestClient http, RagInternalTokenIssuer issuer, ObjectMapper mapper, int maxConcurrentRequests) {
        if (maxConcurrentRequests < 1) {
            throw new IllegalArgumentException("O limite de concorrência RAG deve ser positivo.");
        }
        this.http = http;
        this.issuer = issuer;
        var strictMapper = mapper.copy();
        strictMapper.disable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
        strictMapper.coercionConfigFor(String.class)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
        this.reader = strictMapper.reader()
                .with(StreamReadFeature.STRICT_DUPLICATE_DETECTION.mappedFeature())
                .with(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
                        DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES,
                        DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                        DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS,
                        DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .without(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
        this.capacity = new Semaphore(maxConcurrentRequests);
    }

    public Conversation createConversation() {
        return call(HttpMethod.POST, CONVERSATIONS, Map.of(), 201, Conversation.class);
    }

    public Answer sendMessage(UUID conversationId, TutorMensagemRequestDTO request) {
        Objects.requireNonNull(conversationId, "conversaId");
        Objects.requireNonNull(request, "mensagem");
        if (request.mensagemId() == null || !request.isPerguntaValida()) {
            throw new IllegalArgumentException("ID e pergunta válidos são obrigatórios.");
        }

        var question = new Question(request.mensagemId(), request.pergunta());
        var answer = call(HttpMethod.POST, CONVERSATION + "/messages", question, 200, Answer.class, conversationId);
        if (!conversationId.equals(answer.conversationId()) || !question.messageId().equals(answer.messageId())
                || answer.answer().isBlank() || answer.evidence().size() > 2
                || answer.evidence().isEmpty() != (answer.outcome() == Outcome.NO_EVIDENCE)
                || answer.evidence().stream().anyMatch(RagHttpClient::invalidEvidence)) {
            throw invalidResponse();
        }
        return answer;
    }

    public void closeConversation(UUID conversationId) {
        call(HttpMethod.DELETE, CONVERSATION, null, 204, Void.class,
                Objects.requireNonNull(conversationId, "conversaId"));
    }

    public Liveness liveness() {
        var health = call(HttpMethod.GET, HEALTH + "live", null, 200, Liveness.class);
        if (health.status() != HealthStatus.ALIVE) {
            throw invalidResponse();
        }
        return health;
    }

    public Readiness readiness() {
        var health = call(HttpMethod.GET, HEALTH + "ready", null, 200, Readiness.class);
        if (health.status() != HealthStatus.READY) {
            throw invalidResponse();
        }
        return health;
    }

    private <T> T call(HttpMethod method, String path, Object body, int status, Class<T> type, Object... variables) {
        if (!capacity.tryAcquire()) {
            throw new RagClientException(Code.CONCURRENCY_LIMIT, true);
        }
        try {
            var request = http.method(method).uri(path, variables)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(issuer.issueForAuthenticatedUser()));
            if (body != null) {
                request.contentType(MediaType.APPLICATION_JSON).body(body);
            }
            return request.exchange((sent, reply) -> decode(reply, status, type));
        } catch (ResourceAccessException failure) {
            throw new RagClientException(transportCode(failure), true);
        } catch (RestClientException failure) {
            throw invalidResponse();
        } finally {
            capacity.release();
        }
    }

    private <T> T decode(ClientHttpResponse reply, int expectedStatus, Class<T> type) throws IOException {
        int status = reply.getStatusCode().value();
        if (status == expectedStatus) {
            return type == Void.class ? null : json(reply, type);
        }
        if (status == 401 || status == 403) {
            throw new RagClientException(Code.INTERNAL_AUTHENTICATION, false);
        }
        if (status != 404 && status != 409 && status != 422 && status != 429 && status != 503 && status != 504) {
            throw new RagClientException(Code.UNEXPECTED_STATUS, false);
        }
        var error = json(reply, ErrorEnvelope.class).error();
        Code code = switch (error.code()) {
            case "CONVERSATION_NOT_FOUND" -> matching(status, 404, Code.CONVERSATION_NOT_FOUND);
            case "MESSAGE_CONFLICT" -> matching(status, 409, Code.MESSAGE_CONFLICT);
            case "TURN_IN_PROGRESS" -> matching(status, 409, Code.TURN_IN_PROGRESS);
            case "INVALID_REQUEST" -> matching(status, 422, Code.CONTRACT_REJECTED);
            case "SESSION_LIMIT" -> matching(status, 429, Code.SESSION_LIMIT);
            case "MESSAGE_LIMIT" -> matching(status, 429, Code.MESSAGE_LIMIT);
            case "CONCURRENCY_LIMIT" -> matching(status, 429, Code.CONCURRENCY_LIMIT);
            case "MODEL_UNAVAILABLE" -> matching(status, 503, Code.MODEL_UNAVAILABLE);
            case "INDEX_UNAVAILABLE" -> matching(status, 503, Code.INDEX_UNAVAILABLE);
            case "INDEX_INCOMPATIBLE" -> matching(status, 503, Code.INDEX_INCOMPATIBLE);
            case "PROVIDER_UNAVAILABLE" -> matching(status, 503, Code.PROVIDER_UNAVAILABLE);
            case "PROVIDER_TIMEOUT" -> matching(status, 504, Code.PROVIDER_TIMEOUT);
            default -> throw invalidResponse();
        };
        throw new RagClientException(code, code != Code.CONTRACT_REJECTED && error.retryable());
    }

    private <T> T json(ClientHttpResponse reply, Class<T> type) throws IOException {
        MediaType media;
        try {
            media = reply.getHeaders().getContentType();
        } catch (IllegalArgumentException failure) {
            throw invalidResponse();
        }
        if (media == null || !MediaType.APPLICATION_JSON.isCompatibleWith(media)) {
            throw invalidResponse();
        }
        byte[] bytes = reply.getBody().readNBytes(MAX_RESPONSE_BYTES + 1);
        if (bytes.length > MAX_RESPONSE_BYTES) {
            throw invalidResponse();
        }
        try {
            T value = reader.forType(type).readValue(bytes);
            if (value == null) {
                throw invalidResponse();
            }
            return value;
        } catch (JsonProcessingException | IllegalArgumentException failure) {
            throw invalidResponse();
        }
    }

    private static boolean invalidEvidence(Evidence evidence) {
        return evidence.id().isBlank() || evidence.corpus().isBlank() || evidence.corpusVersion() < 1;
    }

    private static Code matching(int status, int expected, Code code) {
        if (status != expected) {
            throw invalidResponse();
        }
        return code;
    }

    private static Code transportCode(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConnectTimeoutException) {
                return Code.CONNECTION_TIMEOUT;
            }
            if (cause instanceof SocketTimeoutException) {
                return Code.RESPONSE_TIMEOUT;
            }
        }
        return Code.CONNECTION_FAILED;
    }

    private static RagClientException invalidResponse() {
        return new RagClientException(Code.INVALID_RESPONSE, false);
    }
}
