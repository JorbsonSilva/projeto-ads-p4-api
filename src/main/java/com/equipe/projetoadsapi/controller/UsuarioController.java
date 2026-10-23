package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.*;
import com.equipe.projetoadsapi.exception.RecursoNaoEncontradoException;
import com.equipe.projetoadsapi.model.Aptidao;
import com.equipe.projetoadsapi.model.Interesse;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.repository.AptidaoRepository;
import com.equipe.projetoadsapi.repository.InteresseRepository;
import com.equipe.projetoadsapi.security.ResourceOwnershipService;
import com.equipe.projetoadsapi.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CONTROLLER DA ENTIDADE USUÁRIO (API RESTful v1)
 * Rastreabilidade: Fase 2 (DTOs), Fase 3 (RBAC), Fase 4 (Contrato v1 e OpenAPI) - f4-t1, f4-t3
 * 
 * Centraliza e consolida operações sobre dados de usuário e perfil unificado.
 */
@RestController
@RequestMapping({"/api/v1/usuarios", "/api/usuarios"})
@Tag(name = "Usuários (v1)", description = "Operações cadastrais, busca de professores e perfil completo unificado")
public class UsuarioController {

    @Autowired
    private UsuarioService service;

    @Autowired
    private ResourceOwnershipService ownershipService;

    @Autowired
    private InteresseRepository interesseRepository;

    @Autowired
    private AptidaoRepository aptidaoRepository;

    @Operation(summary = "Cadastro de novo usuário", description = "Inicia o fluxo de onboarding com envio de código de 6 dígitos por e-mail.")
    @PostMapping("/cadastro")
    public ResponseEntity<ApiResponse<Map<String, String>>> cadastrar(@Valid @RequestBody UsuarioCadastroRequestDTO dto) {
        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(dto.getNome());
        novoUsuario.setEmail(dto.getEmail());
        novoUsuario.setSenha(dto.getSenha());
        novoUsuario.setTipo(dto.getTipo() != null && !dto.getTipo().isBlank() ? dto.getTipo() : "ALUNO");
        novoUsuario.setCpf(dto.getCpf());
        novoUsuario.setDataNascimento(dto.getDataNascimento());
        novoUsuario.setBioAluno(dto.getBioAluno());
        novoUsuario.setBioProfessor(dto.getBioProfessor());
        novoUsuario.setFotoUrl(dto.getFotoUrl());

        service.cadastrarUsuario(novoUsuario);
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("email", novoUsuario.getEmail()),
                "Código de verificação enviado para o e-mail: " + novoUsuario.getEmail()
        ));
    }

    @Operation(summary = "Reenviar código de verificação", description = "Dispara novamente o código de verificação para o e-mail informado.")
    @PostMapping("/reenviar-verificacao")
    public ResponseEntity<ApiResponse<Map<String, String>>> reenviarVerificacao(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O e-mail é obrigatório.");
        }
        service.reenviarCodigoVerificacao(email);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("email", email), "Código de verificação reenviado com sucesso!"));
    }

    @Operation(summary = "Verificação de e-mail com código OTP", description = "Confirma o código enviado e ativa o usuário no banco de dados.")
    @PostMapping("/verificar-email")
    public ResponseEntity<ApiResponse<UsuarioResponseDTO>> verificarEmail(@RequestBody Map<String, String> body) {
        String email  = body.get("email");
        String codigo = body.get("codigo");
        if (email == null || codigo == null) {
            throw new IllegalArgumentException("E-mail e código de verificação são obrigatórios.");
        }
        Usuario usuarioCriado = service.verificarEmail(email, codigo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(UsuarioResponseDTO.fromEntity(usuarioCriado), "E-mail validado com sucesso!"));
    }

    @Operation(summary = "Login legado", description = "Autenticação legada. Para novos clientes, utilize /api/v1/auth/login.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UsuarioResponseDTO>> loginUsuario(@Valid @RequestBody UsuarioLoginRequestDTO loginDTO) {
        Usuario loginModel = new Usuario();
        loginModel.setEmail(loginDTO.getEmail());
        loginModel.setSenha(loginDTO.getSenha());

        Usuario usuarioAutenticado = service.consultaUsuario(loginModel);
        return ResponseEntity.ok(ApiResponse.ok(UsuarioResponseDTO.fromEntity(usuarioAutenticado), "Login realizado com sucesso"));
    }

    @Operation(summary = "Buscar usuário por ID", description = "Retorna dados cadastrais públicos do usuário sem expor senha.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponseDTO>> buscarPorId(@PathVariable Long id) {
        Usuario usuarioEncontrado = service.buscarPorId(id);
        if (usuarioEncontrado == null) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado com ID: " + id);
        }
        return ResponseEntity.ok(ApiResponse.ok(UsuarioResponseDTO.fromEntity(usuarioEncontrado)));
    }

    @Operation(summary = "Perfil Completo Consolidado", description = "Retorna dados cadastrais, biografia e saberes/interesses do usuário em chamada única.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}/perfil")
    public ResponseEntity<ApiResponse<PerfilCompletoDTO>> obterPerfilCompleto(@PathVariable Long id) {
        Usuario usuario = service.buscarPorId(id);
        if (usuario == null) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado com ID: " + id);
        }

        List<Interesse> interesses = interesseRepository.findByUsuarioId(id);
        List<Aptidao> aptidoes = aptidaoRepository.findByUsuarioId(id);

        PerfilCompletoDTO dto = PerfilCompletoDTO.fromEntities(usuario, interesses, aptidoes);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Perfil consolidado recuperado com sucesso"));
    }

    @Operation(summary = "Atualizar dados cadastrais básicos", description = "Atualiza nome, foto e biografia do usuário autenticado.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponseDTO>> atualizarPerfil(
            @PathVariable Long id, @RequestBody Usuario dadosAtualizados) {
        ownershipService.validarPropriedadeUsuario(id);
        Usuario atualizado = service.atualizarDados(id, dadosAtualizados);
        return ResponseEntity.ok(ApiResponse.ok(UsuarioResponseDTO.fromEntity(atualizado), "Dados atualizados com sucesso"));
    }

    @Operation(summary = "Alterar senha de acesso", description = "Substitui a senha atual por uma nova com hashing BCrypt.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}/senha")
    public ResponseEntity<ApiResponse<Map<String, String>>> trocarSenha(
            @PathVariable Long id,
            @RequestBody Map<String, String> senhas) {
        ownershipService.validarPropriedadeUsuario(id);
        service.alterarSenha(id, senhas);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "OK"), "Senha alterada com sucesso!"));
    }

    @Operation(summary = "Atualizar perfil de aluno com interesses", description = "Salva biografia e lista de IDs de matérias que deseja aprender.")
    @SecurityRequirement(name = "bearerAuth")
    @SuppressWarnings("unchecked")
    @PutMapping("/{id}/aluno")
    public ResponseEntity<ApiResponse<?>> salvarPerfilAluno(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        ownershipService.validarPropriedadeUsuario(id);
        String bioExtraida = (String) payload.get("bio");
        List<Integer> interessesIds = (List<Integer>) payload.get("interessesIds");
        Object resultado = service.atualizarBioAluno(id, bioExtraida, interessesIds);
        return ResponseEntity.ok(ApiResponse.ok(resultado, "Perfil de aluno atualizado com sucesso"));
    }

    @Operation(summary = "Atualizar perfil de professor com aptidões", description = "Salva biografia e lista de matérias lecionadas com preço/hora.")
    @SecurityRequirement(name = "bearerAuth")
    @SuppressWarnings("unchecked")
    @PutMapping("/{id}/professor")
    public ResponseEntity<ApiResponse<?>> salvarPerfilProfessor(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        ownershipService.validarPropriedadeUsuario(id);
        String bioExtraida = (String) payload.get("bio");
        List<Map<String, Object>> aptidoesPayload = (List<Map<String, Object>>) payload.get("aptidoesNova");
        List<Map<String, Object>> dispPayload = (List<Map<String, Object>>) payload.get("disponibilidadesNova");

        Object resultado = service.atualizarPerfilProfessor(id, bioExtraida, aptidoesPayload, dispPayload);
        return ResponseEntity.ok(ApiResponse.ok(resultado, "Perfil de professor atualizado com sucesso"));
    }

    @Operation(summary = "Professores em destaque para o aluno", description = "Recomenda professores baseado nas matérias de interesse do aluno.")
    @GetMapping("/professores/destaque/{alunoId}")
    public ResponseEntity<ApiResponse<List<UsuarioResponseDTO>>> obterProfessoresDestaque(@PathVariable Long alunoId) {
        List<Usuario> professores = service.buscarProfessoresDestaquePorAluno(alunoId);
        List<UsuarioResponseDTO> dtoList = professores.stream()
                .map(UsuarioResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(dtoList, "Professores em destaque recuperados"));
    }
}