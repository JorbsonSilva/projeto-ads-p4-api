package com.equipe.projetoadsapi.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de entrada para autenticação de usuário (compatibilidade com e-mail ou CPF).
 * Rastreabilidade: Fase 2 & [BACK-002]
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioLoginRequestDTO {

    @NotBlank(message = "O e-mail ou CPF é obrigatório para login")
    @JsonAlias({"login", "cpf", "identificador", "username"})
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    private String senha;
}
