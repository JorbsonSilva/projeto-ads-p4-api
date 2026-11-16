package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.Avaliacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    long countByAvaliadoId(Long avaliadoId);
    @EntityGraph(attributePaths = "avaliador")
    @Query("""
            SELECT avaliacao
            FROM Avaliacao avaliacao
            WHERE avaliacao.avaliado.id = :avaliadoId
              AND avaliacao.comentario IS NOT NULL
              AND TRIM(avaliacao.comentario) <> ''
            ORDER BY avaliacao.criadaEm DESC
            """)
    List<Avaliacao> findRecentCommentsByAvaliadoId(
            @Param("avaliadoId") Long avaliadoId,
            Pageable pageable
    );
    @Query("""
            SELECT COUNT(avaliacao)
            FROM Avaliacao avaliacao
            WHERE avaliacao.avaliado.id = :avaliadoId
              AND avaliacao.comentario IS NOT NULL
              AND TRIM(avaliacao.comentario) <> ''
            """)
    long countCommentsByAvaliadoId(@Param("avaliadoId") Long avaliadoId);
    boolean existsByAgendamentoIdAndAvaliadorId(Long agendamentoId, Long avaliadorId);
}
