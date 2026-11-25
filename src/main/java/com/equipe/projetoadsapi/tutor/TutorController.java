package com.equipe.projetoadsapi.tutor;

import com.equipe.projetoadsapi.dto.ApiResponse;
import com.equipe.projetoadsapi.tutor.client.RagClientException;
import com.equipe.projetoadsapi.tutor.dto.TutorConversaDTO;
import com.equipe.projetoadsapi.tutor.dto.TutorEncerramentoDTO;
import com.equipe.projetoadsapi.tutor.dto.TutorErroDTO;
import com.equipe.projetoadsapi.tutor.dto.TutorMensagemRequestDTO;
import com.equipe.projetoadsapi.tutor.dto.TutorRespostaDTO;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tutor/conversas")
public class TutorController {

    private final TutorService tutor;

    public TutorController(TutorService tutor) {
        this.tutor = tutor;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TutorConversaDTO>> criarConversa(@RequestBody JsonNode body) {
        if (body == null || !body.isObject() || !body.isEmpty()) {
            throw new EntradaInvalidaException();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(tutor.criarConversa(), "Conversa criada."));
    }

    @PostMapping("/{conversaId}/mensagens")
    public ApiResponse<TutorRespostaDTO> enviarMensagem(@PathVariable String conversaId,
                                                        @Valid @RequestBody TutorMensagemRequestDTO mensagem) {
        return ApiResponse.ok(tutor.enviarMensagem(uuid(conversaId), mensagem));
    }

    @DeleteMapping("/{conversaId}")
    public ApiResponse<TutorEncerramentoDTO> encerrarConversa(@PathVariable String conversaId) {
        return ApiResponse.ok(tutor.encerrarConversa(uuid(conversaId)), "Conversa encerrada.");
    }

    @ExceptionHandler({EntradaInvalidaException.class, HttpMessageNotReadableException.class,
            MethodArgumentNotValidException.class})
    ResponseEntity<ApiResponse<TutorErroDTO>> entradaInvalida() {
        return erro(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA", "Requisição do tutor inválida.", false);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiResponse<TutorErroDTO>> tipoDeConteudoInvalido() {
        return erro(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FORMATO_INVALIDO", "Envie o corpo em JSON.", false);
    }

    @ExceptionHandler(TutorService.TutorDesativadoException.class)
    ResponseEntity<ApiResponse<TutorErroDTO>> desativado() {
        return erro(HttpStatus.SERVICE_UNAVAILABLE, "TUTOR_DESATIVADO", "Tutor indisponível.", false);
    }

    @ExceptionHandler(RagClientException.class)
    ResponseEntity<ApiResponse<TutorErroDTO>> falhaInterna(RagClientException falha) {
        boolean reenviavel = falha.retryable();
        return switch (falha.code()) {
            case CONVERSATION_NOT_FOUND -> erro(HttpStatus.NOT_FOUND, "CONVERSA_INDISPONIVEL",
                    "Conversa indisponível. Abra uma nova conversa.", false);
            case MESSAGE_CONFLICT -> erro(HttpStatus.CONFLICT, "MENSAGEM_EM_CONFLITO",
                    "O identificador já foi usado com outra pergunta.", false);
            case TURN_IN_PROGRESS -> erro(HttpStatus.CONFLICT, "TURNO_EM_ANDAMENTO",
                    "A conversa já está processando uma mensagem.", reenviavel);
            case SESSION_LIMIT -> limite("LIMITE_DE_CONVERSAS", reenviavel);
            case MESSAGE_LIMIT -> limite("LIMITE_DE_MENSAGENS", reenviavel);
            case CONCURRENCY_LIMIT -> limite("LIMITE_DE_CONCORRENCIA", reenviavel);
            case CONNECTION_TIMEOUT, RESPONSE_TIMEOUT, PROVIDER_TIMEOUT -> erro(HttpStatus.GATEWAY_TIMEOUT,
                    "TUTOR_TIMEOUT", "O tutor excedeu o prazo de resposta.", reenviavel);
            case CONNECTION_FAILED, MODEL_UNAVAILABLE, INDEX_UNAVAILABLE, INDEX_INCOMPATIBLE, PROVIDER_UNAVAILABLE ->
                    erro(HttpStatus.SERVICE_UNAVAILABLE, "TUTOR_INDISPONIVEL",
                            "Tutor temporariamente indisponível.", reenviavel);
            case INTERNAL_AUTHENTICATION, CONTRACT_REJECTED, INVALID_RESPONSE, UNEXPECTED_STATUS ->
                    erro(HttpStatus.BAD_GATEWAY, "FALHA_INTEGRACAO_TUTOR", "Falha na comunicação com o tutor.", false);
        };
    }

    private static ResponseEntity<ApiResponse<TutorErroDTO>> limite(String codigo, boolean reenviavel) {
        return erro(HttpStatus.TOO_MANY_REQUESTS, codigo, "Limite do tutor atingido.", reenviavel);
    }

    private static UUID uuid(String raw) {
        UUID id;
        try {
            id = UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            throw new EntradaInvalidaException();
        }
        if (!id.toString().equalsIgnoreCase(raw)) {
            throw new EntradaInvalidaException();
        }
        return id;
    }

    private static ResponseEntity<ApiResponse<TutorErroDTO>> erro(HttpStatus status, String codigo, String mensagem,
                                                                  boolean reenviavel) {
        return ResponseEntity.status(status).body(ApiResponse.<TutorErroDTO>builder()
                .success(false)
                .status(status.value())
                .message(mensagem)
                .data(new TutorErroDTO(codigo, reenviavel))
                .build());
    }

    private static final class EntradaInvalidaException extends RuntimeException {
    }
}
