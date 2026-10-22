package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.UsuarioCadastroRequestDTO;
import com.equipe.projetoadsapi.dto.UsuarioLoginRequestDTO;
import com.equipe.projetoadsapi.dto.UsuarioResponseDTO;
import com.equipe.projetoadsapi.exception.RecursoNaoEncontradoException;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CONTROLLER DA ENTIDADE USUÁRIO (API RESTful)
 * Rastreabilidade: Fase 2 (Base de Qualidade e Segurança) - f2-t2, f2-t3, f2-t4
 * Entradas validadas com Bean Validation (@Valid) e saídas blindadas com UsuarioResponseDTO
 * para evitar qualquer exposição de senha ou credencial na resposta JSON.
 */
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioService service;

    @Autowired
    private com.equipe.projetoadsapi.security.ResourceOwnershipService ownershipService;

    // ------------------------------------------------------------------
    // ROTA 1 — CADASTRO (Etapa 1: envia o código de verificação por e-mail)
    // ------------------------------------------------------------------
    @PostMapping("/cadastro")
    public ResponseEntity<Map<String, String>> cadastrar(@Valid @RequestBody UsuarioCadastroRequestDTO dto) {
        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(dto.getNome());
        novoUsuario.setEmail(dto.getEmail());
        novoUsuario.setSenha(dto.getSenha());
        novoUsuario.setTipo(dto.getTipo());
        novoUsuario.setCpf(dto.getCpf());
        novoUsuario.setDataNascimento(dto.getDataNascimento());
        novoUsuario.setBioAluno(dto.getBioAluno());
        novoUsuario.setBioProfessor(dto.getBioProfessor());
        novoUsuario.setFotoUrl(dto.getFotoUrl());

        service.cadastrarUsuario(novoUsuario);
        return ResponseEntity.ok(Map.of("message", "Código de verificação enviado para o e-mail: " + novoUsuario.getEmail()));
    }

    // ------------------------------------------------------------------
    // ROTA 2 — VERIFICAÇÃO DE E-MAIL (Etapa 2: confirma o código e salva)
    // ------------------------------------------------------------------
    @PostMapping("/verificar-email")
    public ResponseEntity<UsuarioResponseDTO> verificarEmail(@RequestBody Map<String, String> body) {
        String email  = body.get("email");
        String codigo = body.get("codigo");
        if (email == null || codigo == null) {
            throw new IllegalArgumentException("E-mail e código de verificação são obrigatórios.");
        }
        Usuario usuarioCriado = service.verificarEmail(email, codigo);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponseDTO.fromEntity(usuarioCriado));
    }

    // ------------------------------------------------------------------
    // ROTA 3 — LOGIN
    // ------------------------------------------------------------------
    @PostMapping("/login")
    public ResponseEntity<UsuarioResponseDTO> loginUsuario(@Valid @RequestBody UsuarioLoginRequestDTO loginDTO) {
        Usuario loginModel = new Usuario();
        loginModel.setEmail(loginDTO.getEmail());
        loginModel.setSenha(loginDTO.getSenha());

        Usuario usuarioAutenticado = service.consultaUsuario(loginModel);
        return ResponseEntity.ok(UsuarioResponseDTO.fromEntity(usuarioAutenticado));
    }

    // ------------------------------------------------------------------
    // ROTA 4 — BUSCAR POR ID
    // ------------------------------------------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable Long id) {
        Usuario usuarioEncontrado = service.buscarPorId(id);
        if (usuarioEncontrado == null) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado no banco de dados com ID: " + id);
        }
        return ResponseEntity.ok(UsuarioResponseDTO.fromEntity(usuarioEncontrado));
    }

    // ------------------------------------------------------------------
    // ROTA 5 — ATUALIZAR DADOS PESSOAIS
    // ------------------------------------------------------------------
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> atualizarPerfil(@PathVariable Long id, @RequestBody Usuario dadosAtualizados) {
        ownershipService.validarPropriedadeUsuario(id);
        Usuario atualizado = service.atualizarDados(id, dadosAtualizados);
        return ResponseEntity.ok(UsuarioResponseDTO.fromEntity(atualizado));
    }

    // ------------------------------------------------------------------
    // ROTA 6 — ALTERAR SENHA
    // ------------------------------------------------------------------
    @PutMapping("/{id}/senha")
    public ResponseEntity<Map<String, String>> trocarSenha(@PathVariable Long id,
                                                           @RequestBody Map<String, String> senhas) {
        ownershipService.validarPropriedadeUsuario(id);
        service.alterarSenha(id, senhas);
        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso!"));
    }

    // ------------------------------------------------------------------
    // ROTA 7 — ATUALIZAR PERFIL DO ALUNO
    // ------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    @PutMapping("/{id}/aluno")
    public ResponseEntity<?> salvarPerfilAluno(@PathVariable Long id,
                                               @RequestBody Map<String, Object> payload) {
        String bioExtraida = (String) payload.get("bio");
        List<Integer> interessesIds = (List<Integer>) payload.get("interessesIds");
        return ResponseEntity.ok(service.atualizarBioAluno(id, bioExtraida, interessesIds));
    }

    // ------------------------------------------------------------------
    // ROTA 8 — ATUALIZAR PERFIL DO PROFESSOR
    // ------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    @PutMapping("/{id}/professor")
    public ResponseEntity<?> salvarPerfilProfessor(@PathVariable Long id,
                                                   @RequestBody Map<String, Object> payload) {
        String bioExtraida = (String) payload.get("bio");
        List<Map<String, Object>> aptidoesPayload = (List<Map<String, Object>>) payload.get("aptidoesNova");
        List<Map<String, Object>> dispPayload = (List<Map<String, Object>>) payload.get("disponibilidadesNova");

        return ResponseEntity.ok(service.atualizarPerfilProfessor(id, bioExtraida, aptidoesPayload, dispPayload));
    }

    // ------------------------------------------------------------------
    // ROTA 9 — PROFESSORES EM DESTAQUE
    // ------------------------------------------------------------------
    @GetMapping("/professores/destaque/{alunoId}")
    public ResponseEntity<List<UsuarioResponseDTO>> obterProfessoresDestaque(@PathVariable Long alunoId) {
        List<Usuario> professores = service.buscarProfessoresDestaquePorAluno(alunoId);
        List<UsuarioResponseDTO> dtoList = professores.stream()
                .map(UsuarioResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }
}