package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.Denuncia;
import com.equipe.projetoadsapi.model.StatusDenuncia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório JPA para Denúncias e Ouvidoria.
 * Rastreabilidade: Fase 7 - Bloco K (BACK-016)
 */
@Repository
public interface DenunciaRepository extends JpaRepository<Denuncia, Long> {

    Optional<Denuncia> findByProtocolo(String protocolo);

    List<Denuncia> findAllByOrderByCriadaEmDesc();

    List<Denuncia> findByStatusOrderByCriadaEmDesc(StatusDenuncia status);

    List<Denuncia> findByDenuncianteIdOrderByCriadaEmDesc(Long denuncianteId);

    List<Denuncia> findByDenunciadoIdOrderByCriadaEmDesc(Long denunciadoId);
}
