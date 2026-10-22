package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.*;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.security.JwtAuthenticationFilter;
import com.equipe.projetoadsapi.service.AuthService;
import com.equipe.projetoadsapi.service.CodigoVerificacaoService;
import com.equipe.projetoadsapi.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller de Autenticação e Gestão de Sessões JWT.
 * Rastreabilidade: Fase 3 (Auth & RBAC) - f3-t1..f3-t5
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private CodigoVerificacaoService codigoService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request) {
        AuthResponseDTO response = authService.autenticar(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        AuthResponseDTO response = authService.renovarToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String token = JwtAuthenticationFilter.extrairToken(request);
        authService.logout(token);
        return ResponseEntity.ok(Map.of("message", "Sessão encerrada com sucesso."));
    }

    @PostMapping("/cadastro")
    public ResponseEntity<Map<String, String>> iniciarCadastro(@Valid @RequestBody UsuarioCadastroRequestDTO dto) {
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
        return ResponseEntity.ok(Map.of("message", "Código de verificação enviado para o e-mail: " + novo.getEmail()));
    }

    @PostMapping("/verificar-email")
    public ResponseEntity<AuthResponseDTO> verificarEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String codigo = body.get("codigo");
        if (email == null || codigo == null) {
            throw new IllegalArgumentException("E-mail e código de verificação são obrigatórios.");
        }

        Usuario usuario = usuarioService.verificarEmail(email, codigo);
        AuthRequestDTO loginDTO = new AuthRequestDTO(usuario.getEmail(), usuario.getSenha());
        // Emite token para já deixar logado após verificar e-mail
        AuthResponseDTO authDTO = authService.autenticar(loginDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(authDTO);
    }
}
