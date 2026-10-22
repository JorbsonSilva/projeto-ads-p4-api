package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.CodigoVerificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório de códigos de verificação.
 * Rastreabilidade: Fase 3 (f3-t9)
 */
@Repository
public interface CodigoVerificacaoRepository extends JpaRepository<CodigoVerificacao, Long> {
    Optional<CodigoVerificacao> findTopByEmailAndTipoAndUtilizadoFalseOrderByCreatedAtDesc(String email, String tipo);
}
