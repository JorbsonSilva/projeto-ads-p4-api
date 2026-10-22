package com.equipe.projetoadsapi.model;

/**
 * Papéis do sistema para Role-Based Access Control (RBAC).
 * Rastreabilidade: Fase 3 (Auth & RBAC) - f3-t6
 */
public enum PapelUsuario {
    ROLE_ALUNO,
    ROLE_PROFESSOR,
    ROLE_ADMIN;

    public String getRoleName() {
        return name().replace("ROLE_", "");
    }
}
