package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.AulaEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório de eventos de aula ao vivo.
 * Rastreabilidade: Fase 6 - f6-t5
 */
@Repository
public interface AulaEventoRepository extends JpaRepository<AulaEvento, Long> {
    List<AulaEvento> findBySessaoIdOrderByTimestampServidorAsc(Long sessaoId);
}
