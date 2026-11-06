package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.Saque;
import com.equipe.projetoadsapi.model.enums.StatusSaque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para solicitações de saque financeiro do professor.
 * Rastreabilidade: Fase 7 - Bloco J (BACK-009)
 */
@Repository
public interface SaqueRepository extends JpaRepository<Saque, Long> {

    List<Saque> findByProfessorIdOrderByDataSolicitacaoDesc(Long professorId);

    List<Saque> findByProfessorIdAndStatusSaque(Long professorId, StatusSaque statusSaque);

    Optional<Saque> findByProtocoloSaque(String protocoloSaque);

    boolean existsByProfessorIdAndStatusSaqueIn(Long professorId, Collection<StatusSaque> statuses);

    @Query("SELECT COALESCE(SUM(s.valor), 0) FROM Saque s WHERE s.professor.id = :professorId AND s.statusSaque = :status")
    BigDecimal somarValorPorProfessorEStatus(@Param("professorId") Long professorId, @Param("status") StatusSaque status);

    @Query("SELECT COALESCE(SUM(s.valorLiquido), 0) FROM Saque s WHERE s.professor.id = :professorId AND s.statusSaque = :status")
    BigDecimal somarValorLiquidoPorProfessorEStatus(@Param("professorId") Long professorId, @Param("status") StatusSaque status);
}
