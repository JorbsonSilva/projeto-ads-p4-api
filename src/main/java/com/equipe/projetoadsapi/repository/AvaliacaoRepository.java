package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para registro de avaliações pós-aula.
 * Rastreabilidade: Fase 6 - f6-t8 | Fase 7 - BACK-008
 */
@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    Optional<Avaliacao> findByAgendamentoId(Long agendamentoId);
    Optional<Avaliacao> findByAgendamentoIdAndAvaliadorId(Long agendamentoId, Long avaliadorId);
    List<Avaliacao> findByAvaliadoId(Long avaliadoId);
    boolean existsByAgendamentoIdAndAvaliadorId(Long agendamentoId, Long avaliadorId);
}
