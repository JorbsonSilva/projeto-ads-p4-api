package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.TransacaoFinanceira;
import com.equipe.projetoadsapi.model.enums.TipoTransacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para lançamentos do livro-razão e extrato financeiro.
 * Rastreabilidade: Fase 7 - Bloco J (BACK-009)
 */
@Repository
public interface TransacaoFinanceiraRepository extends JpaRepository<TransacaoFinanceira, Long> {

    List<TransacaoFinanceira> findByUsuarioIdOrderByDataTransacaoDesc(Long usuarioId);

    List<TransacaoFinanceira> findByUsuarioIdAndTipoTransacao(Long usuarioId, TipoTransacao tipoTransacao);

    Optional<TransacaoFinanceira> findByAgendamentoId(Long agendamentoId);

    List<TransacaoFinanceira> findBySaqueId(Long saqueId);

    @Query("SELECT COALESCE(SUM(t.valor), 0) FROM TransacaoFinanceira t WHERE t.usuario.id = :usuarioId AND t.tipoTransacao = :tipo")
    BigDecimal somarPorUsuarioETipo(@Param("usuarioId") Long usuarioId, @Param("tipo") TipoTransacao tipo);
}
