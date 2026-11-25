package com.equipe.projetoadsapi.tutor.security;

import com.equipe.projetoadsapi.security.SecurityUtils;
import io.jsonwebtoken.Jwts;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

public final class RagInternalTokenIssuer {

    private static final String HMAC = "HmacSHA256";

    private final RagIntegrationProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;
    private final SecretKey subjectKey;

    RagInternalTokenIssuer(RagIntegrationProperties properties, Clock clock) {
        if (!"orienta-spring".equals(properties.issuer()) || !"orienta-rag".equals(properties.audience())) {
            throw new IllegalArgumentException("Emissor ou audiência do JWT interno incompatível com o contrato RAG.");
        }
        this.signingKey = secretKey(properties.internalSecret(), "ORIENTA_RAG_INTERNAL_SECRET");
        this.subjectKey = secretKey(properties.subjectSecret(), "ORIENTA_RAG_SUBJECT_SECRET");
        if (signingKey.equals(subjectKey)) {
            throw new IllegalArgumentException("A assinatura e a pseudonimização RAG exigem segredos distintos.");
        }
        this.properties = properties;
        this.clock = clock;
    }

    public String issueForAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = SecurityUtils.getUsuarioAutenticadoId();
        if (authentication == null || !authentication.isAuthenticated() || userId == null || userId <= 0) {
            throw new AccessDeniedException("Principal autenticado do Orienta é obrigatório.");
        }

        Instant issued = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        return Jwts.builder()
                .issuer(properties.issuer())
                .audience().single(properties.audience())
                .subject(pseudonym(userId))
                .claim("scope", "rag:internal")
                .issuedAt(Date.from(issued))
                .expiration(Date.from(issued.plus(properties.tokenTtl())))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    private String pseudonym(long userId) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(subjectKey);
            byte[] digest = mac.doFinal(("orienta-rag:subject:v1:" + userId).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Falha ao gerar o sujeito da integração RAG.", exception);
        }
    }

    private static SecretKey secretKey(String secret, String property) {
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(property + " deve ter pelo menos 32 bytes UTF-8.");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC);
    }
}
