package com.equipe.projetoadsapi.tutor;

import com.equipe.projetoadsapi.model.PapelUsuario;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.security.CustomAccessDeniedHandler;
import com.equipe.projetoadsapi.security.CustomUserDetailsService;
import com.equipe.projetoadsapi.security.JwtAuthenticationEntryPoint;
import com.equipe.projetoadsapi.security.JwtAuthenticationFilter;
import com.equipe.projetoadsapi.security.JwtTokenProvider;
import com.equipe.projetoadsapi.security.SecurityConfig;
import com.equipe.projetoadsapi.security.TokenBlacklistService;
import com.equipe.projetoadsapi.security.UsuarioDetails;
import com.equipe.projetoadsapi.tutor.client.RagHttpClientConfiguration;
import com.equipe.projetoadsapi.tutor.security.RagIntegrationConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named = "ORIENTA_RAG_HTTP_TEST_PYTHON", matches = ".+")
@WebMvcTest(TutorController.class)
@ContextConfiguration(classes = TutorController.class)
@Import({TutorService.class, SecurityConfig.class, JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class, JwtTokenProvider.class,
        RagIntegrationConfiguration.class, RagHttpClientConfiguration.class})
class TutorFastApiIntegrationTest {

    private static final String PATH = "/api/v1/tutor/conversas";
    private static final String SIGNING_KEY = secret();
    private static final String SUBJECT_KEY = secret();
    private static final String QUESTION = "Consigo trocar o horário da mentoria?";
    private static Process python;

    @Autowired MockMvc mvc;
    @Autowired JwtTokenProvider publicTokens;
    @Autowired ObjectMapper mapper;
    @MockitoBean CustomUserDetailsService users;
    @MockitoBean TokenBlacklistService blacklist;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        int port = startFastApi();
        registry.add("orienta.rag.enabled", () -> true);
        registry.add("orienta.rag.base-url", () -> "http://127.0.0.1:" + port);
        registry.add("orienta.rag.internal-secret", () -> SIGNING_KEY);
        registry.add("orienta.rag.subject-secret", () -> SUBJECT_KEY);
    }

    @BeforeEach
    void usuarios() {
        register(1, "um@example.test");
        register(2, "dois@example.test");
    }

    @Test
    void cicloRealComEvidenciaIdempotenciaIsolamentoEEncerramento() throws Exception {
        String owner = token(1, "um@example.test");
        String other = token(2, "dois@example.test");
        String conversation = create(owner);
        UUID messageId = UUID.randomUUID();

        var answered = message(owner, conversation, messageId, QUESTION)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.conversaId").value(conversation))
                .andExpect(jsonPath("$.data.mensagemId").value(messageId.toString()))
                .andExpect(jsonPath("$.data.origem").value("SIMULADO"))
                .andExpect(jsonPath("$.data.resultado").value("RESPONDIDA"))
                .andExpect(jsonPath("$.data.evidencias[0].id").value("M07"))
                .andExpect(jsonPath("$.data.evidencias[0].versaoCorpus").value(1))
                .andReturn();
        var repeated = message(owner, conversation, messageId, QUESTION).andExpect(status().isOk()).andReturn();
        assertEquals(mapper.readTree(answered.getResponse().getContentAsByteArray()).get("data"),
                mapper.readTree(repeated.getResponse().getContentAsByteArray()).get("data"));
        message(owner, conversation, messageId, "Outra pergunta")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.data.codigo").value("MENSAGEM_EM_CONFLITO"));
        message(owner, conversation, UUID.randomUUID(), "E quando?")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.evidencias[0].id").value("M07"));

        message(other, conversation, UUID.randomUUID(), QUESTION)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.data.codigo").value("CONVERSA_INDISPONIVEL"));
        String separate = create(other);
        message(other, separate, UUID.randomUUID(), "E quando?")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.resultado").value("SEM_EVIDENCIA"))
                .andExpect(jsonPath("$.data.evidencias").isEmpty());

        mvc.perform(delete(PATH + "/{id}", conversation).header("Authorization", other))
                .andExpect(status().isNotFound());
        mvc.perform(delete(PATH + "/{id}", conversation).header("Authorization", owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.conversaId").value(conversation));
        message(owner, conversation, UUID.randomUUID(), QUESTION)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.data.codigo").value("CONVERSA_INDISPONIVEL"));
    }

    @Test
    void filtrosAtivosEBloqueioDeCamposControladosPeloApp() throws Exception {
        mvc.perform(post(PATH).contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        String owner = token(1, "um@example.test");
        mvc.perform(post(PATH).header("Authorization", owner)
                        .contentType("application/json").content("{\"usuarioId\":2}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.codigo").value("REQUISICAO_INVALIDA"));
        String conversation = create(owner);
        mvc.perform(post(PATH + "/{id}/mensagens", conversation).header("Authorization", owner)
                        .contentType("application/json").content(mapper.writeValueAsBytes(Map.of(
                                "mensagemId", UUID.randomUUID(), "pergunta", QUESTION, "mode", "semantic_real"))))
                .andExpect(status().isBadRequest());
        message(owner, "1-1-1-1-1", UUID.randomUUID(), QUESTION).andExpect(status().isBadRequest());
        message(owner, conversation, UUID.randomUUID(), "   ").andExpect(status().isBadRequest());
    }

    @Test
    void fastApiExpiraConversaPorInatividade() throws Exception {
        String owner = token(1, "um@example.test");
        String conversation = create(owner);
        Thread.sleep(4_200);
        message(owner, conversation, UUID.randomUUID(), QUESTION)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.codigo").value("CONVERSA_INDISPONIVEL"));
    }

    private String create(String token) throws Exception {
        var response = mvc.perform(post(PATH).header("Authorization", token)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.expiraEm").exists()).andReturn();
        return mapper.readTree(response.getResponse().getContentAsByteArray()).at("/data/conversaId").asText();
    }

    private ResultActions message(String token, String conversation, UUID id, String question) throws Exception {
        return mvc.perform(post(PATH + "/{id}/mensagens", conversation).header("Authorization", token)
                .contentType("application/json").content(mapper.writeValueAsBytes(Map.of(
                        "mensagemId", id, "pergunta", question))));
    }

    private void register(long id, String email) {
        Usuario user = new Usuario();
        user.setId(id);
        user.setEmail(email);
        user.setEmailVerificado(true);
        when(users.loadUserByUsernameAndRole(email, "ROLE_ALUNO"))
                .thenReturn(new UsuarioDetails(user, PapelUsuario.ROLE_ALUNO));
    }

    private String token(long id, String email) {
        return "Bearer " + publicTokens.gerarAccessToken(id, email, "ROLE_ALUNO");
    }

    private static int startFastApi() throws Exception {
        String project = System.getenv().getOrDefault("ORIENTA_RAG_TEST_PROJECT", "../projeto-ads-p4-rag");
        var builder = new ProcessBuilder(System.getenv("ORIENTA_RAG_HTTP_TEST_PYTHON"), "-B", "-u", "-c", """
                import asyncio, socket, uvicorn
                from rag.http_api import create_app

                async def main():
                    with socket.socket() as listener:
                        listener.bind(("127.0.0.1", 0))
                        server = uvicorn.Server(uvicorn.Config(create_app(), log_level="error", access_log=False))
                        task = asyncio.create_task(server.serve(sockets=[listener]))
                        while not server.started and not task.done():
                            await asyncio.sleep(0.02)
                        if server.started:
                            print(listener.getsockname()[1], flush=True)
                        await task

                asyncio.run(main())
                """).directory(Path.of(project).toFile()).redirectError(ProcessBuilder.Redirect.INHERIT);
        builder.environment().clear();
        builder.environment().putAll(Map.of(
                "RAG_INTERNAL_JWT_SECRET", SIGNING_KEY, "RAG_HTTP_MODE", "simulated",
                "RAG_HTTP_IDLE_SECONDS", "4", "HF_HUB_OFFLINE", "1", "PYTHONIOENCODING", "utf-8"));
        python = builder.start();
        try {
            var ready = CompletableFuture.supplyAsync(() -> {
                try {
                    return python.inputReader().readLine();
                } catch (IOException failure) {
                    throw new IllegalStateException("Falha ao ler a prontidão do FastAPI.", failure);
                }
            }).get(30, TimeUnit.SECONDS);
            if (ready == null) {
                throw new IllegalStateException("FastAPI encerrou antes de iniciar.");
            }
            return Integer.parseInt(ready);
        } catch (Exception failure) {
            stopFastApi();
            throw failure;
        }
    }

    @AfterAll
    static void stopFastApi() throws InterruptedException {
        if (python != null && python.isAlive()) {
            python.destroy();
            if (!python.waitFor(5, TimeUnit.SECONDS)) {
                python.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
            }
        }
    }

    private static String secret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
