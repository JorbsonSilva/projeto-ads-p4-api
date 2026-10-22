package com.equipe.projetoadsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resposta de autenticação com Access Token JWT e Refresh Token.
 * Rastreabilidade: Fase 3 (f3-t3)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String token;
    private String refreshToken;
    @Builder.Default
    private String tipoToken = "Bearer";
    private long expiresIn;
    private UsuarioResponseDTO usuario;
}
