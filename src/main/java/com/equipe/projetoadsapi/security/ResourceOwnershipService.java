package com.equipe.projetoadsapi.security;

import com.equipe.projetoadsapi.exception.RegraNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * SERVIÇO DE SEGURANÇA E PROPRIEDADE DE RECURSOS (Anti-ID Spoofing e RBAC RN-001).
 * Rastreabilidade: Fase 3 (Auth & RBAC) - f3-t7, Requisito RN-001
 * 
 * Garante que:
 * 1. Um usuário autenticado só possa acessar, ler ou alterar recursos de sua legítima propriedade.
 * 2. Tentativas de alterar IDs na URL (ex: GET /api/v1/usuarios/999/perfil com token de id 123)
 *    sejam interceptadas, gerem log de auditoria com severidade WARN e retornem HTTP 403 Forbidden.
 * 3. Acesso administrativo (ROLE_ADMIN) possua permissão para suporte e moderação.
 * 4. Docentes não possam contratar mentoria consigo mesmos em modo professor (auto-contratação).
 * 5. Rotas financeiras e de saques sejam restritas exclusivamente ao perfil professor/docente.
 */
@Service("resourceOwnershipService")
public class ResourceOwnershipService {

    private static final Logger log = LoggerFactory.getLogger(ResourceOwnershipService.java);

    /**
     * Valida que o usuário autenticado no SecurityContext é o proprietário do ID informado.
     * Caso contrário, emite log de auditoria de segurança e lança AccessDeniedException (403 Forbidden).
     */
    public void validarPropriedadeUsuario(Long targetUserId) {
        if (targetUserId == null) {
            log.warn("[SEGURANÇA - ID NULO] Tentativa de validação com ID de usuário nulo.");
            throw new AccessDeniedException("Acesso Proibido: Identificador de recurso não informado.");
        }

        Long authenticatedId = SecurityUtils.getUsuarioAutenticadoId();
        if (authenticatedId == null) {
            log.warn("[SEGURANÇA - SESSÃO AUSENTE] Tentativa de acesso sem autenticação válida ao recurso ID={}", targetUserId);
            throw new AccessDeniedException("Usuário não autenticado no contexto de segurança.");
        }

        // ROLE_ADMIN possui permissão global
        if (SecurityUtils.hasRole("ROLE_ADMIN")) {
            return;
        }

        if (!authenticatedId.equals(targetUserId)) {
            log.warn("[SEGURANÇA - AUDITORIA ID SPOOFING DETECTADO] Usuário autenticado id={} tentou acessar ou alterar recurso pertencente ao usuário id={}",
                    authenticatedId, targetUserId);
            throw new AccessDeniedException("Acesso Proibido (ID Spoofing): Você não tem permissão para acessar ou alterar dados de outro usuário.");
        }
    }

    /**
     * Alias semântico para validar se o usuário é o dono do recurso ou administrador.
     */
    public void validarPropriedadeOuAdmin(Long targetUserId) {
        validarPropriedadeUsuario(targetUserId);
    }

    /**
     * Método avaliador para anotações SpEL: @PreAuthorize("@resourceOwnershipService.isDonoDoRecurso(#id)")
     */
    public boolean isDonoDoRecurso(Long targetUserId) {
        if (targetUserId == null) return false;
        Long authenticatedId = SecurityUtils.getUsuarioAutenticadoId();
        if (authenticatedId == null) return false;
        if (SecurityUtils.hasRole("ROLE_ADMIN")) return true;

        boolean matches = authenticatedId.equals(targetUserId);
        if (!matches) {
            log.warn("[SEGURANÇA - SPEL ID SPOOFING DETECTADO] Usuário autenticado id={} bloqueado ao tentar operar sobre id={}",
                    authenticatedId, targetUserId);
        }
        return matches;
    }

    /**
     * Impede expressamente a auto-contratação de mentoria (RN-001):
     * Um professor não pode contratar aula consigo próprio.
     */
    public void validarNaoAutoContratacao(Long alunoId, Long professorId) {
        if (alunoId != null && alunoId.equals(professorId)) {
            log.warn("[SEGURANÇA - AUTO-CONTRATAÇÃO BLOQUEADA] Usuário id={} tentou contratar mentoria consigo mesmo.", alunoId);
            throw new RegraNegocioException("Violação de Regra RN-001: Não é permitido agendar ou contratar mentoria consigo mesmo.");
        }
    }

    /**
     * Valida se o usuário possui papel docente (ROLE_PROFESSOR ou ROLE_ADMIN) e é dono do recurso informado.
     */
    public void validarAcessoDocente(Long professorId) {
        validarPropriedadeUsuario(professorId);
        if (!SecurityUtils.hasRole("ROLE_PROFESSOR") && !SecurityUtils.hasRole("ROLE_ADMIN")) {
            log.warn("[SEGURANÇA - PERFIL INADEQUADO] Usuário id={} tentou acessar recurso restrito a docentes sem papel correspondente.", professorId);
            throw new AccessDeniedException("Acesso Proibido (RN-001): Este recurso é restrito exclusivamente ao perfil docente/professor.");
        }
    }

    /**
     * Valida se o usuário possui papel discente (ROLE_ALUNO ou ROLE_ADMIN) e é dono do recurso informado.
     */
    public void validarAcessoAluno(Long alunoId) {
        validarPropriedadeUsuario(alunoId);
        if (!SecurityUtils.hasRole("ROLE_ALUNO") && !SecurityUtils.hasRole("ROLE_ADMIN")) {
            log.warn("[SEGURANÇA - PERFIL INADEQUADO] Usuário id={} tentou acessar recurso de aluno sem papel correspondente.", alunoId);
            throw new AccessDeniedException("Acesso Proibido (RN-001): Este recurso é restrito exclusivamente ao perfil discente/aluno.");
        }
    }

    /**
     * Valida se o usuário autenticado é o aluno ou o professor de uma sessão/agendamento.
     */
    public void validarParticipanteAgendamento(Long alunoId, Long professorId) {
        Long authenticatedId = SecurityUtils.getUsuarioAutenticadoId();
        if (authenticatedId == null) {
            throw new AccessDeniedException("Usuário não autenticado no contexto de segurança.");
        }

        if (SecurityUtils.hasRole("ROLE_ADMIN")) {
            return;
        }

        if (!authenticatedId.equals(alunoId) && !authenticatedId.equals(professorId)) {
            log.warn("[SEGURANÇA - INVASÃO DE AGENDAMENTO DETECTADA] Usuário autenticado id={} tentou acessar mentoria entre aluno id={} e professor id={}",
                    authenticatedId, alunoId, professorId);
            throw new AccessDeniedException("Acesso Proibido: Você não é participante deste agendamento.");
        }
    }

    /**
     * Avaliador SpEL para participantes de agendamento.
     */
    public boolean isParticipanteAgendamento(Long alunoId, Long professorId) {
        Long authenticatedId = SecurityUtils.getUsuarioAutenticadoId();
        if (authenticatedId == null) return false;
        if (SecurityUtils.hasRole("ROLE_ADMIN")) return true;
        return authenticatedId.equals(alunoId) || authenticatedId.equals(professorId);
    }
}
