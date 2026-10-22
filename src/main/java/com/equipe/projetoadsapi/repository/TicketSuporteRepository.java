package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.StatusTicket;
import com.equipe.projetoadsapi.model.TicketSuporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketSuporteRepository extends JpaRepository<TicketSuporte, Long> {
    List<TicketSuporte> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);
    List<TicketSuporte> findByStatusOrderByCriadoEmDesc(StatusTicket status);
    List<TicketSuporte> findAllByOrderByCriadoEmDesc();
    Optional<TicketSuporte> findByProtocolo(String protocolo);
}
