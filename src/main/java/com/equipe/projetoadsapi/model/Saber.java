package com.equipe.projetoadsapi.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * ENTIDADE SABER (Catálogo de Matérias)
 * Roteiro para a Banca: "Esta é uma tabela de domínio. Ela armazena as disciplinas
 * disponíveis na plataforma de forma única, evitando duplicidade de dados e 
 * padronizando a busca."
 */
@Entity
@Table(name = "tb_saberes")
@Data // Lombok: Gera getters, setters, toString, etc. automaticamente
@Builder
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

    @Column(columnDefinition = "TEXT")
    private String descricao;

    private String icone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "topicos_populares", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> topicosPopulares = new ArrayList<>();

    @Builder.Default
    private Boolean destaque = false;

    @Column(name = "media_preco", precision = 10, scale = 2)
    private BigDecimal mediaPreco;
}