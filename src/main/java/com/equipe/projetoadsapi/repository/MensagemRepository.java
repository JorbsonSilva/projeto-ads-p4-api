package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.Mensagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MensagemRepository extends JpaRepository<Mensagem, Long> {
    List<Mensagem> findByConversaIdOrderByEnviadaEmAsc(Long conversaId);

    @Modifying
    @Query("UPDATE Mensagem m SET m.lida = true, m.lidaEm = :agora WHERE m.conversa.id = :conversaId AND m.remetente.id != :usuarioId AND m.lida = false")
    void marcarMensagensRecebidasComoLidas(@Param("conversaId") Long conversaId, @Param("usuarioId") Long usuarioId, @Param("agora") Instant agora);

    long countByConversaIdAndRemetenteIdNotAndLidaFalse(Long conversaId, Long usuarioId);
}
