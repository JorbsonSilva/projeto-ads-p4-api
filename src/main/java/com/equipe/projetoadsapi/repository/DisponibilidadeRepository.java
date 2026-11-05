package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.Disponibilidade;
import com.equipe.projetoadsapi.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisponibilidadeRepository extends JpaRepository<Disponibilidade, Long> {
    List<Disponibilidade> findByUsuario(Usuario usuario);

    List<Disponibilidade> findByUsuarioId(Long usuarioId);

    void deleteByUsuarioId(Long usuarioId);
}