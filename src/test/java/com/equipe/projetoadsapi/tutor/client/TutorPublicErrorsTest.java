package com.equipe.projetoadsapi.tutor.client;

import com.equipe.projetoadsapi.tutor.TutorController;
import com.equipe.projetoadsapi.tutor.TutorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TutorPublicErrorsTest {

    private static final String PATH = "/api/v1/tutor/conversas";

    @ParameterizedTest
    @CsvSource({
            "INTERNAL_AUTHENTICATION,502,FALHA_INTEGRACAO_TUTOR,false",
            "CONTRACT_REJECTED,502,FALHA_INTEGRACAO_TUTOR,false",
            "INVALID_RESPONSE,502,FALHA_INTEGRACAO_TUTOR,false",
            "CONNECTION_FAILED,503,TUTOR_INDISPONIVEL,true",
            "MODEL_UNAVAILABLE,503,TUTOR_INDISPONIVEL,true",
            "PROVIDER_TIMEOUT,504,TUTOR_TIMEOUT,true",
            "RESPONSE_TIMEOUT,504,TUTOR_TIMEOUT,true",
            "CONVERSATION_NOT_FOUND,404,CONVERSA_INDISPONIVEL,false",
            "MESSAGE_CONFLICT,409,MENSAGEM_EM_CONFLITO,false",
            "TURN_IN_PROGRESS,409,TURNO_EM_ANDAMENTO,true",
            "CONCURRENCY_LIMIT,429,LIMITE_DE_CONCORRENCIA,true",
            "SESSION_LIMIT,429,LIMITE_DE_CONVERSAS,true",
            "MESSAGE_LIMIT,429,LIMITE_DE_MENSAGENS,false"
    })
    void publicaApenasErroSanitizado(String internal, int status, String code, boolean retryable) throws Exception {
        var service = mock(TutorService.class);
        when(service.criarConversa()).thenThrow(new RagClientException(RagClientException.Code.valueOf(internal), retryable));
        MockMvcBuilders.standaloneSetup(new TutorController(service)).build()
                .perform(post(PATH).contentType("application/json").content("{}"))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.data.codigo").value(code))
                .andExpect(jsonPath("$.data.reenviavel").value(retryable))
                .andExpect(jsonPath("$.data.error").doesNotExist());
        verify(service).criarConversa();
        verifyNoMoreInteractions(service);
    }

    @Test
    void desativadoMantemEnvelopeCom503() throws Exception {
        @SuppressWarnings("unchecked")
        ObjectProvider<RagHttpClient> clients = mock(ObjectProvider.class);
        MockMvcBuilders.standaloneSetup(new TutorController(new TutorService(clients))).build()
                .perform(post(PATH).contentType("application/json").content("{}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.data.codigo").value("TUTOR_DESATIVADO"));
    }
}
