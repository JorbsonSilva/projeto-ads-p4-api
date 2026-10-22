package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.Interesse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * REPOSITÓRIO DA ENTIDADE INTERESSE
 * * Defesa para a Banca: "Mapeia as operações da tabela associativa de interesses. 
 * O método 'findByUsuarioId' permite recuperar instantaneamente todos os assuntos 
 * que um aluno específico deseja aprender."
 */
@Repository
public interface InteresseRepository extends JpaRepository<Interesse, Long> {
    
    // Busca todos os interesses vinculados ao ID de um determinado usuário
    List<Interesse> findByUsuarioId(Long usuarioId);
    
    // Deleta todos os interesses de um usuário (útil para quando ele atualizar a lista no perfil)
    void deleteByUsuarioId(Long usuarioId);
}