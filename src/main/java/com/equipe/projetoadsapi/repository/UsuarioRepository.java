package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.Usuario; // Importa a tabela Usuario

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>{

    Usuario findByEmail(String email);
    Usuario findBySenha(String senha);
    // 🟢 BUSCA PROFESSORES FILTRADOS PELAS MATÉRIAS DE INTERESSE E ORDENA PELA MAIOR NOTA
    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.aptidoes a WHERE a.saber.id IN :saberIds ORDER BY u.notaMedia DESC")
    List<Usuario> findProfessoresPorMateriasOrdenadosPorNota(@Param("saberIds") List<Long> saberIds);
    
}
