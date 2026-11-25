package com.equipe.projetoadsapi.tutor.security;

import com.equipe.projetoadsapi.model.PapelUsuario;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.security.UsuarioDetails;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.spec.SecretKeySpec;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RagInternalTokenIssuerTest {

    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");
    private final String signature = secret();
    private final String subject = secret();
    private final ObjectMapper mapper = new ObjectMapper();

    @AfterEach
    void limpaContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assinaHs256ComAudienciaStringEValidadeDeSessentaSegundos() throws Exception {
        autenticar(7L, "conta@example.test");
        String token = issuer().issueForAuthenticatedUser();

        var verifiedJwt = Jwts.parser().verifyWith(key(signature)).clock(() -> Date.from(NOW))
                .build().parseSignedClaims(token);
        assertEquals("HS256", verifiedJwt.getHeader().getAlgorithm());
        assertThrows(SignatureException.class, () -> Jwts.parser().verifyWith(key(secret()))
                .clock(() -> Date.from(NOW)).build().parseSignedClaims(token));
        JsonNode claims = claims(token);
        assertEquals("orienta-spring", claims.path("iss").asText());
        assertTrue(claims.path("aud").isTextual());
        assertEquals("orienta-rag", claims.path("aud").asText());
        assertEquals("rag:internal", claims.path("scope").asText());
        assertTrue(claims.path("iat").isIntegralNumber());
        assertTrue(claims.path("exp").isIntegralNumber());
        assertEquals(NOW.getEpochSecond(), claims.path("iat").asLong());
        assertEquals(60, claims.path("exp").asLong() - claims.path("iat").asLong());
        assertFalse(claims.has("userId"));
        assertFalse(claims.has("email"));
        assertFalse(claims.has("papel"));
    }

    @Test
    void sujeitoEstavelDerivaApenasDoIdDoPrincipal() throws Exception {
        var issuer = issuer();
        autenticar(7L, "conta@example.test");
        String first = claims(issuer.issueForAuthenticatedUser()).path("sub").asText();
        autenticar(7L, "conta-alterada@example.test");
        assertEquals(first, claims(issuer.issueForAuthenticatedUser()).path("sub").asText());
        autenticar(8L, "conta@example.test");
        assertNotEquals(first, claims(issuer.issueForAuthenticatedUser()).path("sub").asText());
        assertNotEquals("7", first);
        assertEquals(43, first.length());
    }

    @Test
    void exigePrincipalOrientaAutenticado() {
        var issuer = issuer();
        assertThrows(AccessDeniedException.class, issuer::issueForAuthenticatedUser);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("usuario", null, java.util.List.of()));
        assertThrows(AccessDeniedException.class, issuer::issueForAuthenticatedUser);
        autenticar(7L, "conta@example.test");
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated(principal, null));
        assertThrows(AccessDeniedException.class, issuer::issueForAuthenticatedUser);
    }

    @Test
    void recusaSegredosInsegurosESemExporSeusValores() {
        for (String invalid : new String[]{null, "", "curto", " ".repeat(40)}) {
            assertThrows(IllegalArgumentException.class, () ->
                    new RagInternalTokenIssuer(properties(invalid, subject), Clock.systemUTC()));
            assertThrows(IllegalArgumentException.class, () ->
                    new RagInternalTokenIssuer(properties(signature, invalid), Clock.systemUTC()));
        }
        assertThrows(IllegalArgumentException.class, () ->
                new RagInternalTokenIssuer(properties(signature, signature), Clock.systemUTC()));
        assertFalse(properties(signature, subject).toString().contains(signature));
        assertFalse(properties(signature, subject).toString().contains(subject));
    }

    @Test
    void tokenJavaEValidadoPeloVerificadorPython() throws Exception {
        String python = System.getenv("ORIENTA_RAG_TEST_PYTHON");
        assumeTrue(python != null && !python.isBlank(), "Defina ORIENTA_RAG_TEST_PYTHON para testar PyJWT localmente.");
        String configuredPath = System.getenv("ORIENTA_RAG_TEST_PROJECT");
        Path project = Path.of(configuredPath == null ? "../projeto-ads-p4-rag" : configuredPath)
                .toAbsolutePath().normalize();
        String script = """
                import os, sys
                from rag.http_auth import InternalAuth
                actual = InternalAuth.from_env().subject(sys.stdin.read().strip())
                if actual != os.environ["ORIENTA_EXPECTED_SUBJECT"]:
                    raise SystemExit(2)
                """;
        var command = new ProcessBuilder(python, "-B", "-c", script).directory(project.toFile())
                .redirectErrorStream(true);
        autenticar(7L, "conta@example.test");
        var issuer = new RagInternalTokenIssuer(properties(signature, subject), Clock.systemUTC());
        String token = issuer.issueForAuthenticatedUser();
        command.environment().put("RAG_INTERNAL_JWT_SECRET", signature);
        command.environment().put("RAG_INTERNAL_JWT_ISSUER", "orienta-spring");
        command.environment().put("RAG_INTERNAL_JWT_AUDIENCE", "orienta-rag");
        command.environment().put("ORIENTA_EXPECTED_SUBJECT", claims(token).path("sub").asText());
        Process process = command.start();
        try (OutputStream input = process.getOutputStream()) {
            input.write(token.getBytes(StandardCharsets.UTF_8));
        }
        try {
            assertTrue(process.waitFor(15, TimeUnit.SECONDS), "O verificador Python excedeu o prazo.");
            assertEquals(0, process.exitValue(), "O verificador Python rejeitou o JWT interno.");
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private RagInternalTokenIssuer issuer() {
        return new RagInternalTokenIssuer(properties(signature, subject), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private JsonNode claims(String token) throws Exception {
        return mapper.readTree(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
    }

    private static RagIntegrationProperties properties(String signature, String subject) {
        return new RagIntegrationProperties(true, URI.create("http://127.0.0.1:8000"), "orienta-spring",
                "orienta-rag", signature, subject, Duration.ofSeconds(60), Duration.ofMillis(500),
                Duration.ofSeconds(3));
    }

    private static SecretKeySpec key(String value) {
        return new SecretKeySpec(value.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    static String secret() {
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private static void autenticar(long id, String email) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        usuario.setEmailVerificado(true);
        var principal = new UsuarioDetails(usuario, PapelUsuario.ROLE_ALUNO);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }
}
