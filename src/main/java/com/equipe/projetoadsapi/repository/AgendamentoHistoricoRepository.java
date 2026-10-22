package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.AgendamentoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório de auditoria para histórico de agendamentos.
 * Rastreabilidade: Fase 5 - f5-t7
 */
@Repository
public interface AgendamentoHistoricoRepository extends JpaRepository<AgendamentoHistorico, Long> {
    List<AgendamentoHistorico> findByAgendamentoIdOrderByDataHoraDesc(Long agendamentoId);
}
