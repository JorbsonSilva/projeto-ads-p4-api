package com.equipe.projetoadsapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload de login com suporte a E-mail ou CPF.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestDTO {

    @NotBlank(message = "O e-mail ou CPF é obrigatório.")
    private String email; // Pode receber tanto o e-mail quanto o CPF digitado

    @NotBlank(message = "A senha é obrigatória.")
    private String senha;
}