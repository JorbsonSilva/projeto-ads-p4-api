package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.Saber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * REPOSITÓRIO DA ENTIDADE SABER
 * * Defesa para a Banca: "Esta interface estende o JpaRepository, o que nos fornece 
 * um CRUD completo de forma nativa. Adicionamos o método personalizado 'findByNome' 
 * utilizando as Derived Queries do Spring Data, permitindo buscar uma disciplina no 
 * banco de dados apenas pelo seu nome textual."
 */
@Repository
public interface SaberRepository extends JpaRepository<Saber, Long> {
    
    // Busca um saber pelo nome (Ex: "Matemática"). Retorna um Optional para evitar o erro NullPointerException.
    Optional<Saber> findByNome(String nome);
}