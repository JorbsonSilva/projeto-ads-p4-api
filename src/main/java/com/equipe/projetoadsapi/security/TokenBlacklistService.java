package com.equipe.projetoadsapi.security;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de revogação de tokens JWT em memória (Blacklist / DT-008).
 * Rastreabilidade: Fase 3 (Auth & RBAC) - f3-t4
 * Armazena assinaturas/jti revogadas até a expiração natural do token.
 */
@Service
public class TokenBlacklistService {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void revogarToken(String token, long expiracaoEpochMillis) {
        if (token != null) {
            blacklist.put(token, expiracaoEpochMillis);
        }
    }

    public boolean isRevogado(String token) {
        if (token == null) return true;
        Long exp = blacklist.get(token);
        if (exp == null) return false;
        if (System.currentTimeMillis() > exp) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }
}
