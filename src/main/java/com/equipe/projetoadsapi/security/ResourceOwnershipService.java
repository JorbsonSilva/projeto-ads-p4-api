package com.equipe.projetoadsapi.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Validação rigorosa de propriedade de recursos contra ID Spoofing.
 * Rastreabilidade: Fase 3 (Auth & RBAC) - f3-t7
 * Impede que um usuário autenticado altere dados enviando o ID de outro usuário na URL.
 */
@Service
public class ResourceOwnershipService {

    public void validarPropriedadeUsuario(Long targetUserId) {
        Long authenticatedId = SecurityUtils.getUsuarioAutenticadoId();
        if (authenticatedId == null) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }

        // Se for ADMIN, permissão total concedida
        if (SecurityUtils.hasRole("ROLE_ADMIN")) {
            return;
        }

        if (!authenticatedId.equals(targetUserId)) {
            throw new AccessDeniedException("Acesso Proibido: Você não tem permissão para visualizar ou alterar dados de outro usuário.");
        }
    }

    public void validarPropriedadeOuAdmin(Long targetUserId) {
        validarPropriedadeUsuario(targetUserId);
    }
}
