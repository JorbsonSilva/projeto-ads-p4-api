package com.equipe.projetoadsapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal; // Importação necessária para trabalhar com valores monetários de forma precisa

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * ENTIDADE APTIDÃO (Relacionamento Professor -> Saber com Atributos Customizados)
 * * Roteiro de Defesa para a Banca:
 * "Esta classe representa uma tabela associativa enriquecida. Em relacionamentos 
 * Muitos-para-Muitos tradicionais, a tabela guarda apenas as chaves estrangeiras. 
 * No entanto, nossa regra de negócio exige atributos específicos para o papel do professor. 
 * Por isso, adicionamos nesta tabela o 'nivelDominio' e o 'precoHora', permitindo que
 * o professor defina um preço diferente e um nível de experiência para cada disciplina que ensina."
 */
@Entity
@Table(name = "tb_aptidoes")
@Data // Lombok: Gera automaticamente Getters, Setters, equals, hashCode e toString
@NoArgsConstructor // Lombok: Gera o construtor padrão sem argumentos, obrigatório para o JPA/Hibernate
@AllArgsConstructor // Lombok: Gera o construtor preenchido com todos os campos
public class Aptidao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Relacionamento com a tabela de Usuários.
     * Muitos registros de aptidão podem apontar para o mesmo Usuário (o Professor).
     */
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnore
    private Usuario usuario;

    /**
     * Relacionamento com a tabela de Saberes (Disciplinas).
     * Muitos registros de aptidão podem apontar para o mesmo Saber (a matéria ensinada).
     */
    @ManyToOne
    @JoinColumn(name = "saber_id", nullable = false)
    private Saber saber;

    /**
     * Grau de conhecimento do professor naquela matéria específica.
     * Exemplo: "Iniciante", "Intermediário", "Avançado".
     */
    @Column(name = "nivel_dominio", nullable = false)
    private String nivelDominio;

    /**
     * 🟢 O VALOR DA HORA-AULA (Adicionado para atender à regra de negócio)
     * * Ponto de Defesa: "Utilizamos o tipo BigDecimal em vez de Double ou Float. 
     * Na engenharia de software financeira, o BigDecimal é o padrão recomendado porque 
     * possui precisão absoluta, evitando problemas de arredondamento em operações matemáticas."
     */
    @Column(name = "preco_hora", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoHora; // No banco, isso criará um campo NUMERIC(10,2) (Ex: 99999999.99)
}