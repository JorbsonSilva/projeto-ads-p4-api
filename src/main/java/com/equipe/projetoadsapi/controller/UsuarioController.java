package com.equipe.projetoadsapi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.service.UsuarioService;

import java.util.Map;

/**
 * CONTROLLER DA ENTIDADE USUÁRIO (API RESTful)
 * * Roteiro de Defesa para a Banca: "Implementamos a arquitetura em camadas rígidas
 * (Controller -> Service -> Repository). O fluxo de cadastro é realizado em duas etapas:
 * (1) POST /cadastro envia o código por e-mail; (2) POST /verificar-email confirma o código
 * e salva o usuário. Isso garante que apenas e-mails válidos acessem a plataforma."
 */
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioService service;

    // ------------------------------------------------------------------
    // ROTA 1 — CADASTRO (Etapa 1: envia o código de verificação por e-mail)
    // ------------------------------------------------------------------
    /**
     * Recebe os dados do usuário, valida duplicidade de e-mail e envia um
     * código de 6 dígitos para o endereço informado.
     * NÃO persiste o registro até a confirmação do código.
     *
     * POST /api/usuarios/cadastro
     * Body: { "nome": "...", "email": "...", "senha": "...", ... }
     * Response 200: "Código de verificação enviado para o seu e-mail."
     */
    @PostMapping("/cadastro")
    public ResponseEntity<?> cadastrar(@RequestBody Usuario novoUsuario) {
        try {
            service.cadastrarUsuario(novoUsuario);
            return ResponseEntity.ok(Map.of("message", "Código de verificação enviado para o e-mail: " + novoUsuario.getEmail()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // ROTA 2 — VERIFICAÇÃO DE E-MAIL (Etapa 2: confirma o código e salva)
    // ------------------------------------------------------------------
    /**
     * Valida o código recebido por e-mail. Se correto, cria o usuário no banco
     * e devolve o objeto persistido (com ID).
     *
     * POST /api/usuarios/verificar-email
     * Body: { "email": "...", "codigo": "123456" }
     * Response 201: { ...usuario salvo... }
     */
    @PostMapping("/verificar-email")
    public ResponseEntity<?> verificarEmail(@RequestBody Map<String, String> body) {
        try {
            String email  = body.get("email");
            String codigo = body.get("codigo");
            Usuario usuarioCriado = service.verificarEmail(email, codigo);
            return ResponseEntity.status(HttpStatus.CREATED).body(usuarioCriado);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // ROTA 3 — LOGIN
    // ------------------------------------------------------------------
    @PostMapping("/login")
    public ResponseEntity<?> loginUsuario(@RequestBody Usuario loginUsuario) {
        try {
            Usuario usuarioAutenticado = service.consultaUsuario(loginUsuario);
            return ResponseEntity.ok(usuarioAutenticado);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // ROTA 4 — BUSCAR POR ID
    // ------------------------------------------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        Usuario usuarioEncontrado = service.buscarPorId(id);
        if (usuarioEncontrado != null) {
            return ResponseEntity.ok(usuarioEncontrado);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Usuário não encontrado no banco de dados."));
    }

    // ------------------------------------------------------------------
    // ROTA 5 — ATUALIZAR DADOS PESSOAIS
    // ------------------------------------------------------------------
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarPerfil(@PathVariable Long id, @RequestBody Usuario dadosAtualizados) {
        try {
            return ResponseEntity.ok(service.atualizarDados(id, dadosAtualizados));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // ROTA 6 — ALTERAR SENHA
    // ------------------------------------------------------------------
    @PutMapping("/{id}/senha")
    public ResponseEntity<?> trocarSenha(@PathVariable Long id,
                                         @RequestBody java.util.Map<String, String> senhas) {
        try {
            service.alterarSenha(id, senhas);
            return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso!"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // ROTA 7 — ATUALIZAR PERFIL DO ALUNO
    // ------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    @PutMapping("/{id}/aluno")
    public ResponseEntity<?> salvarPerfilAluno(@PathVariable Long id,
                                               @RequestBody java.util.Map<String, Object> payload) {
        try {
            String bioExtraida = (String) payload.get("bio");
            java.util.List<Integer> interessesIds = (java.util.List<Integer>) payload.get("interessesIds");
            return ResponseEntity.ok(service.atualizarBioAluno(id, bioExtraida, interessesIds));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

// ------------------------------------------------------------------
    // ROTA 8 — ATUALIZAR PERFIL DO PROFESSOR
    // ------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    @PutMapping("/{id}/professor")
    public ResponseEntity<?> salvarPerfilProfessor(@PathVariable Long id,
                                                   @RequestBody java.util.Map<String, Object> payload) {
        try {
            String bioExtraida = (String) payload.get("bio");
            java.util.List<java.util.Map<String, Object>> aptidoesPayload =
                    (java.util.List<java.util.Map<String, Object>>) payload.get("aptidoesNova");
            
            // 🟢 NOVO: Pegando a lista de horários enviada pelo App Mobile
            java.util.List<java.util.Map<String, Object>> dispPayload =
                    (java.util.List<java.util.Map<String, Object>>) payload.get("disponibilidadesNova");

            // 🟢 ATUALIZADO: Passando o 4º parâmetro (dispPayload)
            return ResponseEntity.ok(service.atualizarPerfilProfessor(id, bioExtraida, aptidoesPayload, dispPayload));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/professores/destaque/{alunoId}")
    public ResponseEntity<java.util.List<Usuario>> obterProfessoresDestaque(@PathVariable Long alunoId) {
        try {
            java.util.List<Usuario> professores = service.buscarProfessoresDestaquePorAluno(alunoId);
            return ResponseEntity.ok(professores);
        } catch (Exception e) {
            System.err.println("[ERRO] /professores/destaque/" + alunoId + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }
}