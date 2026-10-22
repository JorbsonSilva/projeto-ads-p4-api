package com.equipe.projetoadsapi.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utilitários de extração do usuário autenticado no contexto de segurança.
 * Rastreabilidade: Fase 3 (f3-t7)
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static UsuarioDetails getUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioDetails details) {
            return details;
        }
        return null;
    }

    public static Long getUsuarioAutenticadoId() {
        UsuarioDetails details = getUsuarioAutenticado();
        return details != null ? details.getId() : null;
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase(roleWithPrefix));
    }
}
