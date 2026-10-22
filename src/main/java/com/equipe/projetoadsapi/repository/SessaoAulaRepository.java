package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.SessaoAula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório de sessões de aula ao vivo.
 * Rastreabilidade: Fase 6 - f6-t1
 */
@Repository
public interface SessaoAulaRepository extends JpaRepository<SessaoAula, Long> {
    Optional<SessaoAula> findByAgendamentoId(Long agendamentoId);
}
