package com.equipe.projetoadsapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Provedor de geração, validação e extração de tokens JWT.
 * Rastreabilidade: Fase 3 (Auth & RBAC) - f3-t3
 * Emite Access Token (24h) e Refresh Token (7 dias) assinados com HMAC-SHA256.
 */
@Component
public class JwtTokenProvider {

    @Value("${orienta.jwt.secret:orienta-super-secret-key-ads-p4-minimum-32-bytes-long-2026}")
    private String jwtSecret;

    @Value("${orienta.jwt.expiration-ms:86400000}") // 24 horas
    private long jwtExpirationMs;

    @Value("${orienta.jwt.refresh-expiration-ms:604800000}") // 7 dias
    private long refreshExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String gerarAccessToken(Long usuarioId, String email, String papel) {
        Date agora = new Date();
        Date validade = new Date(agora.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("userId", usuarioId)
                .claim("papel", papel)
                .claim("roles", List.of(papel))
                .issuedAt(agora)
                .expiration(validade)
                .signWith(key)
                .compact();
    }

    public String gerarRefreshToken(Long usuarioId, String email) {
        Date agora = new Date();
        Date validade = new Date(agora.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("userId", usuarioId)
                .claim("type", "REFRESH")
                .issuedAt(agora)
                .expiration(validade)
                .signWith(key)
                .compact();
    }

    public boolean validarToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extrairEmail(String token) {
        return extrairClaims(token).getSubject();
    }

    public Long extrairUserId(String token) {
        Number id = extrairClaims(token).get("userId", Number.class);
        return id != null ? id.longValue() : null;
    }

    public String extrairPapel(String token) {
        return extrairClaims(token).get("papel", String.class);
    }

    public Date extrairExpiracao(String token) {
        return extrairClaims(token).getExpiration();
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }
}
