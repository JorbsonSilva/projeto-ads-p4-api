package com.equipe.projetoadsapi.tutor;

import com.equipe.projetoadsapi.tutor.client.RagHttpClient;
import com.equipe.projetoadsapi.tutor.dto.TutorConversaDTO;
import com.equipe.projetoadsapi.tutor.dto.TutorEncerramentoDTO;
import com.equipe.projetoadsapi.tutor.dto.TutorEvidenciaDTO;
import com.equipe.projetoadsapi.tutor.dto.TutorMensagemRequestDTO;
import com.equipe.projetoadsapi.tutor.dto.TutorRespostaDTO;
import com.equipe.projetoadsapi.tutor.dto.TutorRespostaDTO.Origem;
import com.equipe.projetoadsapi.tutor.dto.TutorRespostaDTO.Resultado;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TutorService {

    private final ObjectProvider<RagHttpClient> clients;

    public TutorService(ObjectProvider<RagHttpClient> clients) {
        this.clients = clients;
    }

    public TutorConversaDTO criarConversa() {
        var conversa = cliente().createConversation();
        return new TutorConversaDTO(conversa.conversationId(), conversa.expiresAt().toInstant());
    }

    public TutorRespostaDTO enviarMensagem(UUID conversaId, TutorMensagemRequestDTO mensagem) {
        var resposta = cliente().sendMessage(conversaId, mensagem);
        var resultado = switch (resposta.outcome()) {
            case ANSWERED -> Resultado.RESPONDIDA;
            case NO_EVIDENCE -> Resultado.SEM_EVIDENCIA;
        };
        var origem = switch (resposta.mode()) {
            case DEMONSTRATIVE -> Origem.DEMONSTRATIVO;
            case SIMULATED -> Origem.SIMULADO;
            case SEMANTIC_REAL -> Origem.RAG;
        };
        var evidencias = resposta.evidence().stream()
                .map(evidencia -> new TutorEvidenciaDTO(evidencia.id(), evidencia.corpusVersion()))
                .toList();
        return new TutorRespostaDTO(resposta.conversationId(), resposta.messageId(), resultado,
                resposta.answer(), origem, evidencias);
    }

    public TutorEncerramentoDTO encerrarConversa(UUID conversaId) {
        cliente().closeConversation(conversaId);
        return new TutorEncerramentoDTO(conversaId);
    }

    private RagHttpClient cliente() {
        RagHttpClient client = clients.getIfAvailable();
        if (client == null) {
            throw new TutorDesativadoException();
        }
        return client;
    }

    static final class TutorDesativadoException extends RuntimeException {
    }
}
