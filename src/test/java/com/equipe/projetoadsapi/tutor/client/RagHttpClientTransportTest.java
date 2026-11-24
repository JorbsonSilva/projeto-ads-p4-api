package com.equipe.projetoadsapi.tutor.client;

import com.equipe.projetoadsapi.model.PapelUsuario;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.security.UsuarioDetails;
import com.equipe.projetoadsapi.tutor.security.RagIntegrationConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.equipe.projetoadsapi.tutor.client.RagClientException.Code.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class RagHttpClientTransportTest {

    private static final String CONVERSATIONS = "/internal/v1/conversations";
    private final String signingSecret = newSecret();
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(RagIntegrationConfiguration.class, RagHttpClientConfiguration.class)
            .withBean(ObjectMapper.class, () -> new ObjectMapper().findAndRegisterModules());

    @Test
    void desativadoPorPadraoSemClienteNemCredenciais() {
        context.run(result -> assertThat(result).hasNotFailed().doesNotHaveBean(RagHttpClient.class));
    }

    @Test
    void recusaLimiteDeConcorrenciaNaoPositivo() {
        enabled(8000).withPropertyValues("orienta.rag.max-concurrent-requests=0")
                .run(result -> assertThat(result).hasFailed());
    }

    @Test
    void assinaComEmissorRealERecusaRedirecionamentoSemRepassarBearerPublico() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var capturedToken = new AtomicReference<String>();
        var capturedBody = new AtomicReference<String>();
        var redirected = new AtomicInteger();
        server.createContext(CONVERSATIONS, exchange -> {
            try (exchange) {
                capturedToken.set(exchange.getRequestHeaders().getFirst("Authorization"));
                capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                exchange.getResponseHeaders().set("Location", "/redirected");
                exchange.sendResponseHeaders(307, -1);
            }
        });
        server.createContext("/redirected", exchange -> {
            try (exchange) {
                redirected.incrementAndGet();
                exchange.sendResponseHeaders(200, -1);
            }
        });

        server.start();
        authenticate();
        try {
            enabled(server.getAddress().getPort()).run(result -> {
                var client = result.getBean(RagHttpClient.class);
                assertEquals(UNEXPECTED_STATUS, assertThrows(RagClientException.class, client::createConversation).code());
            });
            assertEquals(0, redirected.get());
            assertEquals("{}", capturedBody.get());
            var authorization = capturedToken.get();
            assertNotNull(authorization);
            assertTrue(authorization.startsWith("Bearer "));
            assertFalse(authorization.contains("bearer-publico-de-teste"));
            var key = new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            var claims = Jwts.parser().requireIssuer("orienta-spring").requireAudience("orienta-rag")
                    .verifyWith(key).build().parseSignedClaims(authorization.substring(7)).getPayload();
            assertEquals("rag:internal", claims.get("scope"));
            assertEquals(43, claims.getSubject().length());
            assertFalse(claims.containsKey("email"));
            assertFalse(claims.containsKey("userId"));
        } finally {
            SecurityContextHolder.clearContext();
            server.stop(0);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void timeoutDeRespostaNoTransporteNaoReenviaPost(boolean sendHeaders) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var requests = new AtomicInteger();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        server.createContext(CONVERSATIONS, exchange -> {
            try (exchange) {
                requests.incrementAndGet();
                exchange.getRequestBody().readAllBytes();
                if (sendHeaders) {
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(201, 128);
                    exchange.getResponseBody().write('{');
                    exchange.getResponseBody().flush();
                }
                entered.countDown();
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
            }
        });

        server.start();
        authenticate();
        try {
            enabled(server.getAddress().getPort()).withPropertyValues("orienta.rag.response-timeout=500ms")
                    .run(result -> {
                        var client = result.getBean(RagHttpClient.class);
                        assertEquals(RESPONSE_TIMEOUT,
                                assertThrows(RagClientException.class, client::createConversation).code());
                        assertTrue(entered.await(1, TimeUnit.SECONDS));
                        assertEquals(1, requests.get());
                    });
        } finally {
            release.countDown();
            SecurityContextHolder.clearContext();
            server.stop(0);
        }
    }

    private ApplicationContextRunner enabled(int port) {
        return context.withPropertyValues(
                "orienta.rag.enabled=true", "orienta.rag.base-url=http://127.0.0.1:" + port,
                "orienta.rag.internal-secret=" + signingSecret,
                "orienta.rag.subject-secret=" + newSecret());
    }

    private static String newSecret() {
        var bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static void authenticate() {
        Usuario user = new Usuario();
        user.setId(7L);
        user.setEmail("perfil@example.test");
        user.setEmailVerificado(true);
        var principal = new UsuarioDetails(user, PapelUsuario.ROLE_ALUNO);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken
                .authenticated(principal, "bearer-publico-de-teste", principal.getAuthorities()));
    }
}
