package com.equipe.projetoadsapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de entrada para cadastro de novo usuário com Bean Validation.
 * Rastreabilidade: Fase 2 (Base de Qualidade e Segurança) - f2-t2, f2-t3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioCadastroRequestDTO {

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 100, message = "O nome deve conter entre 2 e 100 caracteres")
    private String nome;

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "Informe um e-mail válido")
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 6, max = 100, message = "A senha deve conter no mínimo 6 caracteres")
    private String senha;

    @NotBlank(message = "O tipo de usuário (ALUNO ou PROFESSOR) é obrigatório")
    private String tipo;

    @Size(max = 14, message = "O CPF não pode exceder 14 caracteres")
    private String cpf;

    private String dataNascimento;
    private String bioAluno;
    private String bioProfessor;
    private String fotoUrl;
}
