package com.equipe.projetoadsapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload de login com autenticação JWT.
 * Rastreabilidade: Fase 3 (f3-t3)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestDTO {

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Formato de e-mail inválido.")
    private String email;

    @NotBlank(message = "A senha é obrigatória.")
    private String senha;
}
