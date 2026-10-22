package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.Conversa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversaRepository extends JpaRepository<Conversa, Long> {
    Optional<Conversa> findByAgendamentoId(Long agendamentoId);
    boolean existsByAgendamentoId(Long agendamentoId);
}
