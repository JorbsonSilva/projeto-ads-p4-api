package com.equipe.projetoadsapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de entrada para autenticação de usuário.
 * Rastreabilidade: Fase 2 (Base de Qualidade e Segurança) - f2-t2, f2-t3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioLoginRequestDTO {

    @NotBlank(message = "O e-mail é obrigatório para login")
    @Email(message = "Informe um e-mail válido")
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    private String senha;
}
