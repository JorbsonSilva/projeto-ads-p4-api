package com.equipe.projetoadsapi.tutor.security;

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
import com.orienta.api.assistant.controller.AssistantController;
import com.orienta.api.assistant.service.DeterministicAssistantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AssistantController.class, TutorSecurityTest.Probe.class})
@ContextConfiguration(classes = {AssistantController.class, TutorSecurityTest.Probe.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class, JwtTokenProvider.class})
class TutorSecurityTest {

    private static final String PATH = "/api/v1/tutor/probe";
    private static final String EMAIL = "aluno@example.test";

    @Autowired MockMvc mvc;
    @Autowired JwtTokenProvider publicTokens;
    @MockitoBean CustomUserDetailsService users;
    @MockitoBean TokenBlacklistService blacklist;
    @MockitoBean DeterministicAssistantService assistant;

    @Test
    void tutorRequerJwtPublicoDeAcesso() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).header("Authorization", "Bearer invalido")).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).header("Authorization", "Bearer " + publicTokens.gerarRefreshToken(7L, EMAIL)))
                .andExpect(status().isUnauthorized());

        var user = new Usuario();
        user.setId(7L);
        user.setEmail(EMAIL);
        user.setEmailVerificado(true);
        when(users.loadUserByUsernameAndRole(EMAIL, "ROLE_ALUNO"))
                .thenReturn(new UsuarioDetails(user, PapelUsuario.ROLE_ALUNO));
        String token = publicTokens.gerarAccessToken(7L, EMAIL, "ROLE_ALUNO");
        mvc.perform(get(PATH).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        when(blacklist.isRevogado(token)).thenReturn(true);
        mvc.perform(get(PATH).header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void preservaPermissoesDosQuatroPrefixosDoAssistente() throws Exception {
        mvc.perform(get("/api/v1/assistente/faq")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/assistant/faq")).andExpect(status().isOk());
        mvc.perform(get("/api/assistente/faq")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/assistant/faq")).andExpect(status().isUnauthorized());
    }

    @RestController
    static class Probe {
        @GetMapping(PATH)
        String probe() {
            return "ok";
        }
    }
}
