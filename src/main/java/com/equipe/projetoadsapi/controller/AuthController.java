package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.*;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.security.JwtAuthenticationFilter;
import com.equipe.projetoadsapi.service.AuthService;
import com.equipe.projetoadsapi.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller de Autenticação e Gestão de Sessões JWT (v1).
 * Rastreabilidade: Fase 3 (Auth & RBAC), Fase 4 (Contrato v1 e OpenAPI) - f4-t1, f4-t5
 */
@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
@Tag(name = "Autenticação (v1)", description = "Login, emissão e renovação de JWT, logout com blacklist e verificação de conta")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioService usuarioService;

    @Operation(summary = "Login com e-mail e senha", description = "Autentica o usuário via BCrypt e retorna accessToken (1h) e refreshToken (7d).")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@Valid @RequestBody AuthRequestDTO request) {
        AuthResponseDTO response = authService.autenticar(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Autenticação realizada com sucesso"));
    }

    @Operation(summary = "Renovação de Token (Refresh)", description = "Emite um novo accessToken válido a partir do refreshToken.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        AuthResponseDTO response = authService.renovarToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(response, "Token renovado com sucesso"));
    }

    @Operation(summary = "Logout de sessão", description = "Invalida o token JWT no servidor adicionando-o à blacklist em memória.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(HttpServletRequest request) {
        String token = JwtAuthenticationFilter.extrairToken(request);
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "LOGGED_OUT"), "Sessão encerrada com sucesso."));
    }

    @Operation(summary = "Iniciar cadastro de usuário", description = "Registra intenção de cadastro e dispara código de 6 dígitos via e-mail.")
    @PostMapping("/cadastro")
    public ResponseEntity<ApiResponse<Map<String, String>>> iniciarCadastro(@Valid @RequestBody UsuarioCadastroRequestDTO dto) {
        Usuario novo = new Usuario();
        novo.setNome(dto.getNome());
        novo.setEmail(dto.getEmail());
        novo.setSenha(dto.getSenha());
        novo.setTipo(dto.getTipo());
        novo.setCpf(dto.getCpf());
        novo.setDataNascimento(dto.getDataNascimento());
        novo.setBioAluno(dto.getBioAluno());
        novo.setBioProfessor(dto.getBioProfessor());
        novo.setFotoUrl(dto.getFotoUrl());

        usuarioService.cadastrarUsuario(novo);
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("email", novo.getEmail()),
                "Código de verificação enviado para o e-mail: " + novo.getEmail()
        ));
    }

    @Operation(summary = "Verificar e-mail com código e autenticar", description = "Valida o código de 6 dígitos e já retorna os tokens JWT prontos para uso.")
    @PostMapping("/verificar-email")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> verificarEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String codigo = body.get("codigo");
        if (email == null || codigo == null) {
            throw new IllegalArgumentException("E-mail e código de verificação são obrigatórios.");
        }

        Usuario usuario = usuarioService.verificarEmail(email, codigo);
        AuthRequestDTO loginDTO = new AuthRequestDTO(usuario.getEmail(), usuario.getSenha());
        AuthResponseDTO authDTO = authService.autenticar(loginDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(authDTO, "Conta verificada com sucesso! Login efetuado."));
    }
}
