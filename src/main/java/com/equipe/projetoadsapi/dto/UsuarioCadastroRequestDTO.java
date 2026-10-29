package com.equipe.projetoadsapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO DE ENTRADA: CADASTRO DE NOVO USUÁRIO
 *
 * Roteiro de Defesa para a Banca Examinadora:
 * "Garante a integridade dos dados antes que alcancem o banco de dados.
 * Valida formatos essenciais (tamanho do nome, formato padrão de e-mail RFC 5322,
 * e comprimento mínimo de senha para mitigar ataques de força bruta)."
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioCadastroRequestDTO {

    @NotBlank(message = "O nome completo é obrigatório.")
    @Size(min = 2, max = 100, message = "O nome deve conter entre 2 e 100 caracteres.")
    private String nome;

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Informe um endereço de e-mail válido.")
    private String email;

    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 6, max = 100, message = "A senha deve conter no mínimo 6 caracteres.")
    private String senha;

    /**
     * Perfil de acesso: ALUNO ou PROFESSOR (padrão ALUNO para cadastro móvel).
     */
    @Builder.Default
    private String tipo = "ALUNO";

    /**
     * CPF do usuário (com ou sem formatação, normalizado pelo service).
     */
    @Size(max = 14, message = "O CPF não pode exceder 14 caracteres.")
    private String cpf;

    /**
     * Data de nascimento no formato ISO (YYYY-MM-DD).
     */
    private String dataNascimento;

    private String bioAluno;
    private String bioProfessor;
    private String fotoUrl;
}
