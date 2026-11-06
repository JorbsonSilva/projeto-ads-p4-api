package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.Conversa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversaRepository extends JpaRepository<Conversa, Long> {
    Optional<Conversa> findByAgendamentoId(Long agendamentoId);
    boolean existsByAgendamentoId(Long agendamentoId);

    @Query("SELECT c FROM Conversa c WHERE (c.agendamento.aluno.id = :usuarioId OR c.agendamento.professor.id = :usuarioId) AND (c.ativa IS NULL OR c.ativa = true) ORDER BY c.atualizadaEm DESC")
    List<Conversa> findByUsuarioParticipante(@Param("usuarioId") Long usuarioId);
}
