package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.*;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.security.JwtAuthenticationFilter;
import com.equipe.projetoadsapi.security.JwtTokenProvider;
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
 * CONTROLADOR REST DE AUTENTICAÇÃO E GESTÃO DE SESSÕES JWT (v1)
 *
 * Roteiro de Defesa para a Banca Examinadora:
 * "Esta classe centraliza os pontos de entrada REST para o ciclo de vida de acesso do aplicativo Orienta.
 * Implementa autenticação baseada em tokens JWT sem estado (stateless), permitindo escalabilidade horizontal.
 * Ao registrar uma conta, emite código de 6 dígitos e, na verificação, emite o accessToken com validade de 1h
 * e refreshToken com validade de 7 dias, garantindo segurança e excelente experiência de usuário."
 *
 * Arquivo compartilhado entre: Tela de Login e Tela de Cadastro.
 */
@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
@CrossOrigin(origins = "*") // Permite requisições do Expo, emuladores Android/iOS e Web
@Tag(name = "Autenticação (v1)", description = "Login unificado, cadastro com verificação de e-mail, renovação de JWT e logout")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    /**
     * ENDPOINT DE LOGIN (CONSUMIDO PELA TELA DE LOGIN):
     * Recebe e-mail ou CPF e a senha. Valida credenciais via BCrypt e retorna
     * os tokens JWT para armazenamento no SecureStore do dispositivo móvel.
     */
    @Operation(summary = "Login com e-mail ou CPF e senha", description = "Autentica o usuário via BCrypt e retorna accessToken (1h) e refreshToken (7d).")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@Valid @RequestBody AuthRequestDTO request) {
        AuthResponseDTO response = authService.autenticar(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Autenticação realizada com sucesso"));
    }

    /**
     * ENDPOINT DE REFRESH DE TOKEN:
     * Emite um novo accessToken válido a partir de um refreshToken íntegro,
     * evitando que o usuário precise digitar suas credenciais novamente a cada expiração.
     */
    @Operation(summary = "Renovação de Token (Refresh)", description = "Emite um novo accessToken válido a partir do refreshToken.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        AuthResponseDTO response = authService.renovarToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(response, "Token renovado com sucesso"));
    }

    /**
     * ENDPOINT DE LOGOUT:
     * Revoga o token atual adicionando sua assinatura à lista negra (TokenBlacklistService)
     * em memória do servidor até a sua data natural de expiração.
     */
    @Operation(summary = "Logout de sessão", description = "Invalida o token JWT no servidor adicionando-o à blacklist em memória.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(HttpServletRequest request) {
        String token = JwtAuthenticationFilter.extrairToken(request);
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "LOGGED_OUT"), "Sessão encerrada com sucesso."));
    }

    /**
     * ENDPOINT DE CADASTRO (CONSUMIDO PELA TELA DE CADASTRO):
     * Recebe nome, e-mail, cpf, data de nascimento, senha e perfil (ALUNO por padrão).
     * Valida unicidade, criptografa a senha e dispara o código de 6 dígitos por e-mail.
     */
    @Operation(summary = "Iniciar cadastro de usuário", description = "Registra intenção de cadastro, valida unicidade de CPF/e-mail e dispara código de 6 dígitos via e-mail.")
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

    /**
     * ENDPOINT DE VERIFICAÇÃO DE E-MAIL COM CÓDIGO DE 6 DÍGITOS:
     * Valida o código digitado. Se correto, efetiva o registro no PostgreSQL e
     * já gera os tokens JWT para que o aplicativo entre logado imediatamente.
     */
    @Operation(summary = "Verificar e-mail com código e autenticar", description = "Valida o código de 6 dígitos, persiste no banco e já retorna os tokens JWT prontos para uso.")
    @PostMapping("/verificar-email")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> verificarEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String codigo = body.get("codigo");

        if (email == null || codigo == null) {
            throw new IllegalArgumentException("E-mail e código de verificação são obrigatórios.");
        }

        Usuario usuario = usuarioService.verificarEmail(email, codigo);

        // 🟢 Gera a sessão JWT para o aluno/professor entrar autenticado na hora
        String papelStr = usuario.getPapel() != null ? usuario.getPapel().name() : "ROLE_ALUNO";
        String accessToken = tokenProvider.gerarAccessToken(usuario.getId(), usuario.getEmail(), papelStr);
        String refreshToken = tokenProvider.gerarRefreshToken(usuario.getId(), usuario.getEmail());

        AuthResponseDTO authDTO = AuthResponseDTO.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .tipoToken("Bearer")
                .expiresIn(tokenProvider.getJwtExpirationMs() / 1000)
                .usuario(UsuarioResponseDTO.fromEntity(usuario))
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(authDTO, "Conta verificada com sucesso! Bem-vindo ao Orienta."));
    }

    /**
     * ENDPOINT DE REENVIO DE CÓDIGO DE VERIFICAÇÃO:
     * Dispara um novo código numérico caso o aluno/professor não tenha recebido.
     */
    @Operation(summary = "Reenviar código de verificação de cadastro")
    @PostMapping("/reenviar-codigo")
    public ResponseEntity<ApiResponse<Map<String, String>>> reenviarCodigo(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O e-mail é obrigatório.");
        }

        usuarioService.reenviarCodigoVerificacao(email);

        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("email", email),
                "Novo código de verificação enviado para o seu e-mail."
        ));
    }
}
