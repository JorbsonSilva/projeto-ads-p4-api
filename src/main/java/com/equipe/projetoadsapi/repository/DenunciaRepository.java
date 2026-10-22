package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.Denuncia;
import com.equipe.projetoadsapi.model.StatusDenuncia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DenunciaRepository extends JpaRepository<Denuncia, Long> {
    List<Denuncia> findByStatusOrderByCriadaEmDesc(StatusDenuncia status);
    List<Denuncia> findByDenuncianteIdOrderByCriadaEmDesc(Long denuncianteId);
    List<Denuncia> findByDenunciadoIdOrderByCriadaEmDesc(Long denunciadoId);
}
