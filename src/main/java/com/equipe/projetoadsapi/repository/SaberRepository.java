package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.dto.CategoriaPopularDTO;
import com.equipe.projetoadsapi.model.PapelUsuario;
import com.equipe.projetoadsapi.model.Saber;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    @Query("""
            SELECT new com.equipe.projetoadsapi.dto.CategoriaPopularDTO(saber.categoria, COUNT(DISTINCT professor.id))
            FROM Aptidao aptidao
            JOIN aptidao.saber saber
            JOIN aptidao.usuario professor
            WHERE (professor.papel = :papel OR :papel MEMBER OF professor.papeis)
              AND saber.categoria IS NOT NULL
              AND TRIM(saber.categoria) <> ''
            GROUP BY saber.categoria
            ORDER BY COUNT(DISTINCT professor.id) DESC, saber.categoria ASC
            """)
    List<CategoriaPopularDTO> findCategoriasMaisEnsinadas(
            @Param("papel") PapelUsuario papel,
            Pageable pageable
    );
}