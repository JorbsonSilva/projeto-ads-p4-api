package com.equipe.projetoadsapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ENTIDADE USUÁRIO (Mapeamento Objeto-Relacional)
 * * Roteiro de Defesa para a Banca: "Utilizamos a especificação JPA com o framework Hibernate 
 * para fazer o mapeamento da nossa classe Java diretamente para a tabela 'tb_usuarios' do Supabase. 
 * Adicionamos restrições como 'unique = true' nos campos de E-mail e CPF para garantir a integridade 
 * dos dados no nível do banco, impedindo cadastros duplicados de forma nativa."
 */
@Entity // Diz que é uma tabela do banco
@Table(name = "tb_usuarios") // Dá o nome da tabela
@Getter // Cria os gets de tudo (Lombok)
@Setter // Cria os sets de tudo (Lombok)
@NoArgsConstructor // Construtor vazio para o banco de dados
@AllArgsConstructor // Construtor cheio para você usar no código
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    // 🟢 DICA DE SEGURANÇA: Faz a senha ser aceita ao salvar, mas nunca enviada de volta pro celular
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    @Column(name = "senha", nullable = false)
    private String senha;

    @Column(name = "cpf", unique = true, length = 14)
    private String cpf;

    @Column(name = "data_nascimento", length = 10)
    private String dataNascimento;

    @Column(name = "bio_aluno", columnDefinition = "TEXT")
    private String bioAluno;

    @Column(name = "bio_professor", columnDefinition = "TEXT")
    private String bioProfessor;

    @Column(name = "foto_url", length = 1000)
    private String fotoUrl;

    @Column(name = "nota_media")
    private Double notaMedia = 5.0; // Professor recém-cadastrado começa com 5 estrelas

    @Column(name = "total_avaliacoes")
    private Integer totalAvaliacoes = 0; // Começa com 0 avaliações

    // 🟢 CORREÇÃO: Envia a lista para o Front-end, mas quebra o loop infinito ignorando apenas o 'usuario' do outro lado
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("usuario")
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Interesse> interesses;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("usuario")
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Aptidao> aptidoes;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("usuario")
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Disponibilidade> disponibilidades;

}