package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.Mensagem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repositório de Mensagens do Chat Interno.
 * Rastreabilidade: Fase 7 - Bloco K (BACK-013, BACK-014)
 */
@Repository
public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

    List<Mensagem> findByConversaIdOrderByEnviadaEmAsc(Long conversaId);

    Page<Mensagem> findByConversaIdOrderByEnviadaEmAsc(Long conversaId, Pageable pageable);

    List<Mensagem> findByConversaAgendamentoIdOrderByEnviadaEmAsc(Long agendamentoId);

    Page<Mensagem> findByConversaAgendamentoIdOrderByEnviadaEmAsc(Long agendamentoId, Pageable pageable);

    Optional<Mensagem> findFirstByConversaIdOrderByEnviadaEmDesc(Long conversaId);

    @Modifying
    @Query("UPDATE Mensagem m SET m.lida = true, m.lidaEm = :agora WHERE m.conversa.id = :conversaId AND m.remetente.id != :usuarioId AND m.lida = false")
    int marcarMensagensRecebidasComoLidas(@Param("conversaId") Long conversaId, @Param("usuarioId") Long usuarioId, @Param("agora") Instant agora);

    @Modifying
    @Query("UPDATE Mensagem m SET m.lida = true, m.lidaEm = :agora WHERE m.conversa.agendamento.id = :agendamentoId AND m.remetente.id != :usuarioId AND m.lida = false")
    int marcarMensagensRecebidasPorAgendamentoComoLidas(@Param("agendamentoId") Long agendamentoId, @Param("usuarioId") Long usuarioId, @Param("agora") Instant agora);

    long countByConversaIdAndRemetenteIdNotAndLidaFalse(Long conversaId, Long usuarioId);
}
