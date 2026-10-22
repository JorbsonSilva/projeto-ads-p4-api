package com.equipe.projetoadsapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENTIDADE INTERESSE (Relacionamento Aluno -> Saber)
 * Roteiro para a Banca: "Esta é uma tabela associativa (N:M). Ela materializa
 * o interesse de um usuário em aprender uma disciplina específica."
 */
@Entity
@Table(name = "tb_interesses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Interesse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacionamento: Muitos interesses podem pertencer a Um usuário
    @ManyToOne 
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Relacionamento: Muitos interesses podem apontar para Um mesmo Saber
    @ManyToOne 
    @JoinColumn(name = "saber_id", nullable = false)
    private Saber saber;
}