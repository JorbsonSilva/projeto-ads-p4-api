package com.equipe.projetoadsapi.security;

import com.equipe.projetoadsapi.exception.RegraNegocioException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de Rate Limiting para proteção contra ataques de força bruta.
 * Rastreabilidade: Fase 3 (Auth & RBAC) - f3-t8
 * Limites: Máximo 5 tentativas de login em 15 minutos; 3 reenvios de código por hora.
 */
@Service
public class RateLimitingService {

    private static class Tentativas {
        int count;
        long janelaInicio;

        Tentativas(long agora) {
            this.count = 1;
            this.janelaInicio = agora;
        }
    }

    private final Map<String, Tentativas> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, Tentativas> resendAttempts = new ConcurrentHashMap<>();

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOGIN_WINDOW_MS = 15 * 60 * 1000L; // 15 minutos

    private static final int MAX_RESEND_ATTEMPTS = 3;
    private static final long RESEND_WINDOW_MS = 60 * 60 * 1000L; // 1 hora

    public void verificarTentativaLogin(String identificador) {
        long agora = System.currentTimeMillis();
        loginAttempts.compute(identificador, (key, tent) -> {
            if (tent == null || (agora - tent.janelaInicio) > LOGIN_WINDOW_MS) {
                return new Tentativas(agora);
            }
            if (tent.count >= MAX_LOGIN_ATTEMPTS) {
                long minutosRestantes = ((tent.janelaInicio + LOGIN_WINDOW_MS) - agora) / 60000 + 1;
                throw new RegraNegocioException("Muitas tentativas de login falhas. Tente novamente em " + minutosRestantes + " minutos.");
            }
            tent.count++;
            return tent;
        });
    }

    public void limparTentativasLogin(String identificador) {
        loginAttempts.remove(identificador);
    }

    public void verificarTentativaReenvioCodigo(String email) {
        long agora = System.currentTimeMillis();
        resendAttempts.compute(email, (key, tent) -> {
            if (tent == null || (agora - tent.janelaInicio) > RESEND_WINDOW_MS) {
                return new Tentativas(agora);
            }
            if (tent.count >= MAX_RESEND_ATTEMPTS) {
                throw new RegraNegocioException("Limite de reenvio de código excedido para este e-mail. Aguarde 1 hora.");
            }
            tent.count++;
            return tent;
        });
    }
}
