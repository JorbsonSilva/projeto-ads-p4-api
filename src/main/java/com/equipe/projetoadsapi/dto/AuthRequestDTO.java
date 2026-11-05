package com.equipe.projetoadsapi.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO DE ENTRADA: REQUISIÇÃO DE AUTENTICAÇÃO (LOGIN)
 * Suporta e-mail ou CPF (formatado ou apenas numérico).
 * Rastreabilidade: [BACK-002]
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestDTO {

    /**
     * Identificador do usuário: aceita e-mail, CPF, login ou identificador.
     */
    @NotBlank(message = "O e-mail ou CPF é obrigatório para autenticação.")
    @JsonAlias({"login", "cpf", "identificador", "username"})
    private String email;

    /**
     * Senha informada pelo usuário em texto puro.
     */
    @NotBlank(message = "A senha é obrigatória.")
    private String senha;
}
