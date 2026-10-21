package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.Aptidao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * REPOSITÓRIO DA ENTIDADE APTIDÃO
 * * Defesa para a Banca: "Gerencia a persistência das competências dos professores. 
 * Contém métodos para listar as aptidões de um docente ou limpar seus registros 
 * antes de uma atualização completa."
 */
@Repository
public interface AptidaoRepository extends JpaRepository<Aptidao, Long> {
    
    // Retorna a lista de matérias que um determinado professor leciona
    List<Aptidao> findByUsuarioId(Long usuarioId);
    
    // Remove todas as aptidões vinculadas ao ID do usuário
    void deleteByUsuarioId(Long usuarioId);
}     