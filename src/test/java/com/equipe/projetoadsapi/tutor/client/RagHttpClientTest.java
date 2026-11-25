package com.equipe.projetoadsapi.tutor.client;

import com.equipe.projetoadsapi.tutor.dto.TutorMensagemRequestDTO;
import com.equipe.projetoadsapi.tutor.security.RagInternalTokenIssuer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.equipe.projetoadsapi.tutor.client.RagClientException.Code.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RagHttpClientTest {

    private static final String BASE = "http://127.0.0.1:8000/internal/v1";
    private static final UUID CONVERSATION = UUID.randomUUID();
    private static final UUID MESSAGE = UUID.randomUUID();
    private static final String MESSAGES = BASE + "/conversations/" + CONVERSATION + "/messages";
    private static final String CREDENTIAL = "credencial-interna-de-teste";
    private static final String SECRET_IN_UPSTREAM = "detalhe-privado-do-upstream";
    private final TutorMensagemRequestDTO question = new TutorMensagemRequestDTO(MESSAGE, " Como remarcar? ");
    private RagInternalTokenIssuer issuer;
    private MockRestServiceServer server;
    private RagHttpClient client;

    @BeforeEach
    void setup() {
        var builder = RestClient.builder().baseUrl("http://127.0.0.1:8000");
        server = MockRestServiceServer.bindTo(builder).build();
        issuer = mock(RagInternalTokenIssuer.class);
        when(issuer.issueForAuthenticatedUser()).thenReturn(CREDENTIAL);
        client = new RagHttpClient(builder.build(), issuer, new ObjectMapper().findAndRegisterModules(), 1);
    }

    @Test
    void criaConsultaSaudeEEncerraComCredencialInternaPorChamada() {
        server.expect(requestTo(BASE + "/conversations")).andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + CREDENTIAL))
                .andExpect(content().json("{}"))
                .andRespond(withStatus(HttpStatusCode.valueOf(201)).contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"conversationId":"%s","mode":"simulated","expiresAt":"2026-11-24T16:00:00Z"}
                                """.formatted(CONVERSATION)));
        server.expect(requestTo(BASE + "/health/live")).andExpect(method(GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + CREDENTIAL))
                .andRespond(withSuccess("{\"status\":\"alive\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/health/ready")).andExpect(method(GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + CREDENTIAL))
                .andRespond(withSuccess("{\"status\":\"ready\",\"mode\":\"simulated\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/conversations/" + CONVERSATION)).andExpect(method(DELETE))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + CREDENTIAL))
                .andRespond(withNoContent());

        var conversation = client.createConversation();
        assertEquals(CONVERSATION, conversation.conversationId());
        assertEquals(RagInternalDtos.Mode.SIMULATED, conversation.mode());
        assertEquals("2026-11-24T16:00Z", conversation.expiresAt().toString());
        assertEquals(RagInternalDtos.HealthStatus.ALIVE, client.liveness().status());
        assertEquals(RagInternalDtos.Mode.SIMULATED, client.readiness().mode());
        client.closeConversation(CONVERSATION);
        verify(issuer, times(4)).issueForAuthenticatedUser();
        server.verify();
    }

    @Test
    void preservaMessageIdEEnviaSomenteContratoInternoComEvidencia() {
        server.expect(requestTo(MESSAGES)).andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + CREDENTIAL))
                .andExpect(content().json("""
                        {"messageId":"%s","question":"Como remarcar?"}
                        """.formatted(MESSAGE), org.springframework.test.json.JsonCompareMode.STRICT))
                .andRespond(withSuccess(answer("answered", """
                        [{"id":"M07","corpus":"orienta_faq","corpusVersion":1}]
                        """), MediaType.APPLICATION_JSON));
        var response = client.sendMessage(CONVERSATION, question);
        assertEquals(MESSAGE, response.messageId());
        assertEquals(RagInternalDtos.Outcome.ANSWERED, response.outcome());
        assertEquals("M07", response.evidence().get(0).id());
        assertEquals(RagInternalDtos.Mode.SIMULATED, response.mode());
        server.verify();
    }

    @Test
    void semEvidenciaContinuaSendoRespostaValida() {
        server.expect(requestTo(MESSAGES))
                .andRespond(withSuccess(answer("no_evidence", "[]"), MediaType.APPLICATION_JSON));
        var response = client.sendMessage(CONVERSATION, question);
        assertEquals(RagInternalDtos.Outcome.NO_EVIDENCE, response.outcome());
        assertTrue(response.evidence().isEmpty());
        server.verify();
    }

    @ParameterizedTest
    @CsvSource({
            "401, INVALID_CREDENTIAL, INTERNAL_AUTHENTICATION, false",
            "403, INSUFFICIENT_SCOPE, INTERNAL_AUTHENTICATION, false",
            "404, CONVERSATION_NOT_FOUND, CONVERSATION_NOT_FOUND, false",
            "409, MESSAGE_CONFLICT, MESSAGE_CONFLICT, false",
            "409, TURN_IN_PROGRESS, TURN_IN_PROGRESS, true",
            "422, INVALID_REQUEST, CONTRACT_REJECTED, false",
            "429, SESSION_LIMIT, SESSION_LIMIT, true",
            "429, MESSAGE_LIMIT, MESSAGE_LIMIT, false",
            "429, CONCURRENCY_LIMIT, CONCURRENCY_LIMIT, true",
            "503, MODEL_UNAVAILABLE, MODEL_UNAVAILABLE, true",
            "503, INDEX_UNAVAILABLE, INDEX_UNAVAILABLE, true",
            "503, INDEX_INCOMPATIBLE, INDEX_INCOMPATIBLE, true",
            "503, PROVIDER_UNAVAILABLE, PROVIDER_UNAVAILABLE, true",
            "504, PROVIDER_TIMEOUT, PROVIDER_TIMEOUT, true"
    })
    void traduzFalhasSemVazarCorpoOuReenviar(int status, String internalCode, String expected, boolean retryable) {
        server.expect(requestTo(MESSAGES)).andRespond(withStatus(HttpStatusCode.valueOf(status))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"error":{"code":"%s","message":"%s","retryable":%s}}
                        """.formatted(internalCode, SECRET_IN_UPSTREAM, retryable)));
        var error = assertThrows(RagClientException.class, () -> client.sendMessage(CONVERSATION, question));
        assertEquals(expected, error.code().name());
        assertEquals(retryable, error.retryable());
        assertFalse(error.getMessage().contains(SECRET_IN_UPSTREAM));
        assertFalse(error.getMessage().contains(CREDENTIAL));
        assertNull(error.getCause());
        verify(issuer).issueForAuthenticatedUser();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not json",
            "null",
            "{\"status\":\"alive\"}{\"status\":\"alive\"}",
            "{\"status\":\"alive\",\"status\":\"alive\"}",
            "{\"status\":7}",
            "{\"status\":\"ready\"}",
            "{}"
    })
    void rejeitaJsonEEstadoDeSaudeInvalidos(String body) {
        server.expect(requestTo(BASE + "/health/live"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertEquals(INVALID_RESPONSE, assertThrows(RagClientException.class, client::liveness).code());
        server.verify();
    }

    @ParameterizedTest
    @CsvSource({
            "conversationId, 08e168f9-5bcd-4de0-9fac-ed8de70f6551",
            "messageId, 08e168f9-5bcd-4de0-9fac-ed8de70f6551",
            "messageId, inválido",
            "mode, real",
            "outcome, desconhecido",
            "answer, ' '"
    })
    void rejeitaCamposObrigatoriosInvalidosOuIdsIncompativeis(String field, String value) throws Exception {
        var mapper = new ObjectMapper();
        var body = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(answer("no_evidence", "[]"));
        body.put(field, value);
        server.expect(requestTo(MESSAGES)).andRespond(withSuccess(body.toString(), MediaType.APPLICATION_JSON));
        assertEquals(INVALID_RESPONSE,
                assertThrows(RagClientException.class, () -> client.sendMessage(CONVERSATION, question)).code());
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[null]", "[{\"id\":\"M07\",\"corpus\":\"faq\",\"corpusVersion\":0}]"})
    void rejeitaRespostaAfirmativaSemEvidenciaValida(String evidence) {
        server.expect(requestTo(MESSAGES))
                .andRespond(withSuccess(answer("answered", evidence), MediaType.APPLICATION_JSON));
        assertEquals(INVALID_RESPONSE,
                assertThrows(RagClientException.class, () -> client.sendMessage(CONVERSATION, question)).code());
        server.verify();
    }

    @Test
    void diferenciaTimeoutDeConexaoDeConexaoRecusada() {
        server.expect(requestTo(BASE + "/health/live"))
                .andRespond(withException(new ConnectTimeoutException("detalhe da conexão")));
        server.expect(requestTo(BASE + "/health/live"))
                .andRespond(withException(new ConnectException("detalhe da conexão")));
        assertEquals(CONNECTION_TIMEOUT, assertThrows(RagClientException.class, client::liveness).code());
        assertEquals(CONNECTION_FAILED, assertThrows(RagClientException.class, client::liveness).code());
        server.verify();
    }

    @Test
    void rejeitaErroComCodigoIncompativelComStatus() {
        server.expect(requestTo(MESSAGES)).andRespond(withStatus(HttpStatusCode.valueOf(503))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":\"MESSAGE_CONFLICT\",\"message\":\"x\",\"retryable\":false}}"));
        assertEquals(INVALID_RESPONSE,
                assertThrows(RagClientException.class, () -> client.sendMessage(CONVERSATION, question)).code());
        server.verify();
    }

    @Test
    void limiteRejeitaSemFilaELiberaVagaAposFalha() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        server.expect(requestTo(BASE + "/health/live")).andRespond(request -> {
            entered.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("Teste não liberou a chamada.");
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new AssertionError(error);
            }
            return withSuccess("inválido", MediaType.APPLICATION_JSON).createResponse(request);
        });
        server.expect(requestTo(BASE + "/health/live"))
                .andRespond(withSuccess("{\"status\":\"alive\"}", MediaType.APPLICATION_JSON));

        var executor = Executors.newSingleThreadExecutor();
        try {
            var first = executor.submit(() -> assertThrows(RagClientException.class, client::liveness));
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            assertEquals(CONCURRENCY_LIMIT, assertThrows(RagClientException.class, client::liveness).code());
            release.countDown();
            assertEquals(INVALID_RESPONSE, first.get(5, TimeUnit.SECONDS).code());
            assertEquals(RagInternalDtos.HealthStatus.ALIVE, client.liveness().status());
            verify(issuer, times(2)).issueForAuthenticatedUser();
            server.verify();
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private static String answer(String outcome, String evidence) {
        return """
                {"conversationId":"%s","messageId":"%s","mode":"simulated",
                 "outcome":"%s","answer":"Orientação de teste","evidence":%s}
                """.formatted(CONVERSATION, MESSAGE, outcome, evidence);
    }
}
