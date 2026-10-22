package com.equipe.projetoadsapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENTIDADE SABER (Catálogo de Matérias)
 * Roteiro para a Banca: "Esta é uma tabela de domínio. Ela armazena as disciplinas
 * disponíveis na plataforma de forma única, evitando duplicidade de dados e 
 * padronizando a busca."
 */
@Entity
@Table(name = "tb_saberes")
@Data // Lombok: Gera getters, setters, toString, etc. automaticamente
@NoArgsConstructor // Lombok: Cria o construtor vazio exigido pelo JPA
@AllArgsConstructor // Lombok: Cria o construtor com todos os atributos
public class Saber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome; // Ex: "Matemática", "Inglês"

    @Column(nullable = false)
    private String categoria; // Ex: "Exatas", "Idiomas"
}