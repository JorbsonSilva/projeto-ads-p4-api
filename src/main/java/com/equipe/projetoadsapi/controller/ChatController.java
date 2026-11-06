package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.*;
import com.equipe.projetoadsapi.security.ResourceOwnershipService;
import com.equipe.projetoadsapi.security.SecurityUtils;
import com.equipe.projetoadsapi.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CONTROLLER DE CHAT INTERNO DA AULA (v1).
 * Rastreabilidade: Fase 7 - Bloco K (BACK-013, BACK-014)
 * 
 * Enforcing de segurança e governança:
 * 1. Previne que um usuário acesse ou envie mensagens fingindo ser outro usuário (Anti-ID Spoofing).
 * 2. Valida que o usuário seja participante legítimo da mentoria/conversa.
 * 3. Garante marcação em lote e automática de status de leitura (duplo tique).
 * 4. Suporta paginação cronológica e metadados de anexos.
 */
@RestController
@RequestMapping({"/api/v1/chat", "/api/chat"})
@Tag(name = "Chat Interno da Aula (v1)", description = "Comunicação contextual à mentoria restrita a aluno e professor a custo zero")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ResourceOwnershipService ownershipService;

    @Operation(summary = "Caixa de entrada unificada de conversas", description = "Retorna a lista de conversas ativas do usuário com interlocutor, última mensagem e contador de mensagens não lidas.")
    @GetMapping("/conversas")
    public ResponseEntity<ApiResponse<List<ConversaDTO>>> listarConversas(
            @RequestParam(required = false) Long usuarioId) {
        Long authenticatedId = SecurityUtils.getUsuarioAutenticadoId();
        Long idAlvo = (usuarioId != null) ? usuarioId : authenticatedId;

        if (idAlvo == null) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }

        // Se passar usuarioId explícito diferente do logado, valida ownership (evita ID Spoofing)
        if (authenticatedId != null && !authenticatedId.equals(idAlvo)) {
            ownershipService.validarPropriedadeUsuario(idAlvo);
        }

        List<ConversaDTO> conversas = chatService.listarConversas(idAlvo);
        return ResponseEntity.ok(ApiResponse.ok(conversas, "Caixa de entrada recuperada com sucesso"));
    }

    @Operation(summary = "Obter dados da conversa por agendamento", description = "Retorna os metadados da conversa vinculada ao agendamento.")
    @GetMapping({"/agendamento/{agendamentoId}", "/agendamentos/{agendamentoId}"})
    public ResponseEntity<ApiResponse<ConversaDTO>> obterConversa(
            @PathVariable Long agendamentoId,
            @RequestParam(required = false) Long usuarioId) {
        Long idAlvo = obterEValidarUsuarioAlvo(usuarioId);
        ConversaDTO conversa = chatService.obterOuCriarConversa(agendamentoId, idAlvo);
        return ResponseEntity.ok(ApiResponse.ok(conversa, "Conversa recuperada"));
    }

    @Operation(summary = "Obter conversa por ID direto", description = "Retorna os metadados da conversa por seu identificador.")
    @GetMapping("/conversas/{conversaId}")
    public ResponseEntity<ApiResponse<ConversaDTO>> obterConversaPorId(
            @PathVariable Long conversaId,
            @RequestParam(required = false) Long usuarioId) {
        Long idAlvo = obterEValidarUsuarioAlvo(usuarioId);
        ConversaDTO conversa = chatService.obterConversaPorId(conversaId, idAlvo);
        return ResponseEntity.ok(ApiResponse.ok(conversa, "Conversa recuperada"));
    }

    @Operation(summary = "Listar histórico de mensagens por agendamento", description = "Retorna mensagens em ordem cronológica e marca automaticamente as mensagens recebidas como lidas.")
    @GetMapping({"/agendamento/{agendamentoId}/mensagens", "/agendamentos/{agendamentoId}/mensagens"})
    public ResponseEntity<ApiResponse<List<MensagemDTO>>> listarMensagens(
            @PathVariable Long agendamentoId,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Long idAlvo = obterEValidarUsuarioAlvo(usuarioId);

        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("enviadaEm").ascending());
            Page<MensagemDTO> pageResult = chatService.listarMensagensPaginadasPorAgendamento(agendamentoId, idAlvo, pageable);
            return ResponseEntity.ok(ApiResponse.ok(pageResult.getContent(), "Mensagens recuperadas com sucesso"));
        }

        List<MensagemDTO> mensagens = chatService.listarMensagens(agendamentoId, idAlvo);
        return ResponseEntity.ok(ApiResponse.ok(mensagens, "Mensagens recuperadas"));
    }

    @Operation(summary = "Listar histórico de mensagens por conversa", description = "Retorna mensagens em ordem cronológica por conversa com confirmação de leitura.")
    @GetMapping("/conversas/{conversaId}/mensagens")
    public ResponseEntity<ApiResponse<List<MensagemDTO>>> listarMensagensPorConversa(
            @PathVariable Long conversaId,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Long idAlvo = obterEValidarUsuarioAlvo(usuarioId);

        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("enviadaEm").ascending());
            Page<MensagemDTO> pageResult = chatService.listarMensagensPaginadasPorConversa(conversaId, idAlvo, pageable);
            return ResponseEntity.ok(ApiResponse.ok(pageResult.getContent(), "Mensagens recuperadas com sucesso"));
        }

        List<MensagemDTO> mensagens = chatService.listarMensagensPorConversa(conversaId, idAlvo);
        return ResponseEntity.ok(ApiResponse.ok(mensagens, "Mensagens recuperadas"));
    }

    @Operation(summary = "Enviar mensagem unificada", description = "Persiste a nova mensagem por agendamento ou conversa, suportando anexos e tipos diversos.")
    @PostMapping("/mensagens")
    public ResponseEntity<ApiResponse<MensagemDTO>> enviarMensagemUnificada(
            @Valid @RequestBody MensagemRequestDTO dto) {
        if (dto.getRemetenteId() != null) {
            ownershipService.validarPropriedadeUsuario(dto.getRemetenteId());
        }
        MensagemDTO salva = chatService.enviarMensagemUnificada(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(salva, "Mensagem enviada com sucesso"));
    }

    @Operation(summary = "Enviar mensagem na conversa por agendamento", description = "Persiste nova mensagem vinculada ao agendamento e notifica a outra parte.")
    @PostMapping({"/agendamento/{agendamentoId}/mensagens", "/agendamentos/{agendamentoId}/mensagens"})
    public ResponseEntity<ApiResponse<MensagemDTO>> enviarMensagem(
            @PathVariable Long agendamentoId,
            @Valid @RequestBody EnviarMensagemDTO dto) {
        if (dto.getRemetenteId() != null) {
            ownershipService.validarPropriedadeUsuario(dto.getRemetenteId());
        }
        MensagemDTO salva = chatService.enviarMensagem(agendamentoId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(salva, "Mensagem enviada com sucesso"));
    }

    @Operation(summary = "Enviar mensagem na conversa por conversaId", description = "Persiste nova mensagem vinculada diretamente à conversa.")
    @PostMapping("/conversas/{conversaId}/mensagens")
    public ResponseEntity<ApiResponse<MensagemDTO>> enviarMensagemPorConversa(
            @PathVariable Long conversaId,
            @Valid @RequestBody EnviarMensagemDTO dto) {
        if (dto.getRemetenteId() != null) {
            ownershipService.validarPropriedadeUsuario(dto.getRemetenteId());
        }
        MensagemRequestDTO request = MensagemRequestDTO.builder()
                .conversaId(conversaId)
                .remetenteId(dto.getRemetenteId())
                .conteudo(dto.getConteudo())
                .tipo(dto.getTipo())
                .anexoUrl(dto.getAnexoUrl())
                .nomeArquivo(dto.getNomeArquivo())
                .tamanhoArquivo(dto.getTamanhoArquivo())
                .build();

        MensagemDTO salva = chatService.enviarMensagemUnificada(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(salva, "Mensagem enviada com sucesso"));
    }

    @Operation(summary = "Marcar mensagens da conversa como lidas", description = "Atualiza em lote o status de leitura para lida = true nas mensagens recebidas da conversa.")
    @PatchMapping("/conversas/{conversaId}/ler")
    public ResponseEntity<ApiResponse<Integer>> marcarMensagensComoLidas(
            @PathVariable Long conversaId,
            @RequestParam(required = false) Long usuarioId) {
        Long idAlvo = obterEValidarUsuarioAlvo(usuarioId);
        int total = chatService.marcarMensagensComoLidas(conversaId, idAlvo);
        return ResponseEntity.ok(ApiResponse.ok(total, total + " mensagens marcadas como lidas"));
    }

    @Operation(summary = "Marcar mensagens do agendamento como lidas", description = "Atualiza em lote o status de leitura para lida = true nas mensagens recebidas do agendamento.")
    @PatchMapping({"/agendamento/{agendamentoId}/ler", "/agendamentos/{agendamentoId}/ler"})
    public ResponseEntity<ApiResponse<Integer>> marcarMensagensAgendamentoComoLidas(
            @PathVariable Long agendamentoId,
            @RequestParam(required = false) Long usuarioId) {
        Long idAlvo = obterEValidarUsuarioAlvo(usuarioId);
        int total = chatService.marcarMensagensComoLidasPorAgendamento(agendamentoId, idAlvo);
        return ResponseEntity.ok(ApiResponse.ok(total, total + " mensagens marcadas como lidas"));
    }

    private Long obterEValidarUsuarioAlvo(Long usuarioId) {
        Long authenticatedId = SecurityUtils.getUsuarioAutenticadoId();
        Long idAlvo = (usuarioId != null) ? usuarioId : authenticatedId;

        if (idAlvo == null) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }

        if (authenticatedId != null && !authenticatedId.equals(idAlvo)) {
            ownershipService.validarPropriedadeUsuario(idAlvo);
        }

        return idAlvo;
    }
}
