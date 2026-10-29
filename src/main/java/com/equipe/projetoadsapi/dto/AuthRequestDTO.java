package com.equipe.projetoadsapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO DE ENTRADA: REQUISIÇÃO DE AUTENTICAÇÃO (LOGIN)
 *
 * Roteiro de Defesa para a Banca Examinadora:
 * "Este Data Transfer Object (DTO) implementa validação de entrada declarativa via Jakarta Bean Validation.
 * O campo 'email' atua como identificador polimórfico, aceitando tanto endereços de e-mail
 * quanto números de CPF (11 dígitos ou formatados), refletindo a experiência de usuário da tela de Login."
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestDTO {

    /**
     * Identificador do usuário: pode ser o e-mail cadastrado ou o CPF.
     */
    @NotBlank(message = "O e-mail ou CPF é obrigatório para autenticação.")
    private String email;

    /**
     * Senha informada pelo usuário em texto puro, que será conferida contra o hash BCrypt no banco.
     */
    @NotBlank(message = "A senha é obrigatória.")
    private String senha;
}
