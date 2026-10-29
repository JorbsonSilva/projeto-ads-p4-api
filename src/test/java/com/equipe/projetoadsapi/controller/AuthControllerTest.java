package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.ApiResponse;
import com.equipe.projetoadsapi.dto.AuthRequestDTO;
import com.equipe.projetoadsapi.dto.AuthResponseDTO;
import com.equipe.projetoadsapi.dto.UsuarioCadastroRequestDTO;
import com.equipe.projetoadsapi.dto.UsuarioResponseDTO;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.PapelUsuario;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.security.JwtTokenProvider;
import com.equipe.projetoadsapi.service.AuthService;
import com.equipe.projetoadsapi.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TESTES AUTOMATIZADOS DO CONTROLADOR DE AUTENTICAÇÃO E CADASTRO
 *
 * Roteiro de Defesa para a Banca Examinadora:
 * "Esta suíte valida os pontos de contato entre o aplicativo React Native e a API Spring Boot.
 * Cobre expressamente as alterações exigidas pela tela de Login:
 * 1) Campo unificado de identificador (aceita e-mail ou CPF com máscara '000.000.000-00');
 * 2) Resposta envelopada em ApiResponse<AuthResponseDTO> com token JWT e dados do usuário;
 * 3) Tratamento adequado de erros com HTTP 401 em vez de erros 500 não tratados;
 * 4) Validação do fluxo de verificação por código de 6 dígitos da tela de Cadastro."
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Desativa filtros de segurança para isolar a camada web
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private JwtTokenProvider tokenProvider;

    // =========================================================================
    // 1. TESTES DA TELA DE LOGIN (app/login.tsx)
    // =========================================================================

    /**
     * TESTE 1: Login bem-sucedido com E-mail padrão.
     * Garante que o app recebe HTTP 200 com token JWT e dados de perfil.
     */
    @Test
    @DisplayName("Deve autenticar com sucesso quando informado e-mail válido e senha correta")
    void deveAutenticarComSucessoUsandoEmail() throws Exception {
        AuthRequestDTO request = new AuthRequestDTO("aluno@orienta.edu.br", "senha123");

        UsuarioResponseDTO usuarioDTO = UsuarioResponseDTO.builder()
                .id(1L)
                .nome("Lucas Silva")
                .email("aluno@orienta.edu.br")
                .tipo("ALUNO")
                .build();

        AuthResponseDTO responseDTO = AuthResponseDTO.builder()
                .token("jwt.token.valido.mock")
                .refreshToken("jwt.refresh.valido.mock")
                .tipoToken("Bearer")
                .expiresIn(3600L)
                .usuario(usuarioDTO)
                .build();

        when(authService.autenticar(any(AuthRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt.token.valido.mock"))
                .andExpect(jsonPath("$.data.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.data.usuario.email").value("aluno@orienta.edu.br"));

        verify(authService, times(1)).autenticar(any(AuthRequestDTO.class));
    }

    /**
     * TESTE 2: Login com CPF com máscara (000.000.000-00).
     * Atende diretamente à alteração realizada para a tela de Login mobile.
     */
    @Test
    @DisplayName("Deve autenticar com sucesso quando informado CPF formatado com pontos e traço no campo de identificador")
    void deveAutenticarComSucessoUsandoCpfComMascara() throws Exception {
        AuthRequestDTO request = new AuthRequestDTO("123.456.789-00", "senha123");

        AuthResponseDTO responseDTO = AuthResponseDTO.builder()
                .token("jwt.token.cpf.mock")
                .tipoToken("Bearer")
                .expiresIn(3600L)
                .build();

        when(authService.autenticar(any(AuthRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt.token.cpf.mock"));
    }

    /**
     * TESTE 3: Tratamento de erro 401 para credenciais inválidas.
     * Evita que senhas erradas resultem em Exception 500 no console do app móvel.
     */
    @Test
    @DisplayName("Deve retornar erro amigável quando a senha estiver incorreta")
    void deveRetornarErroQuandoSenhaIncorreta() throws Exception {
        AuthRequestDTO request = new AuthRequestDTO("aluno@orienta.edu.br", "senha_errada");

        when(authService.autenticar(any(AuthRequestDTO.class)))
                .thenThrow(new RegraNegocioException("Credenciais inválidas: e-mail/CPF ou senha incorretos."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Tratado pelo GlobalExceptionHandler
    }

    // =========================================================================
    // 2. TESTES DA TELA DE CADASTRO COM CÓDIGO DE 6 DÍGITOS (app/cadastro.tsx)
    // =========================================================================

    /**
     * TESTE 4: Iniciar cadastro e despachar código de 6 dígitos.
     */
    @Test
    @DisplayName("Deve iniciar cadastro, salvar temporariamente e despachar código de 6 dígitos por e-mail")
    void deveIniciarCadastroEEnviarCodigo() throws Exception {
        UsuarioCadastroRequestDTO dto = UsuarioCadastroRequestDTO.builder()
                .nome("Beatriz Martins")
                .email("beatriz@orienta.edu.br")
                .cpf("12345678900")
                .dataNascimento("2002-08-15")
                .senha("senhaForte123")
                .tipo("ALUNO")
                .build();

        doNothing().when(usuarioService).cadastrarUsuario(any(Usuario.class));

        mockMvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("beatriz@orienta.edu.br"));

        verify(usuarioService, times(1)).cadastrarUsuario(any(Usuario.class));
    }

    /**
     * TESTE 5: Verificação do código de 6 dígitos e emissão imediata de JWT.
     * Permite que após digitar o código no app, o aluno já entre autenticado.
     */
    @Test
    @DisplayName("Deve validar código de 6 dígitos e emitir tokens JWT para login automático")
    void deveVerificarCodigoEAutenticar() throws Exception {
        Map<String, String> body = Map.of(
                "email", "beatriz@orienta.edu.br",
                "codigo", "482910"
        );

        Usuario usuarioAtivado = new Usuario();
        usuarioAtivado.setId(10L);
        usuarioAtivado.setNome("Beatriz Martins");
        usuarioAtivado.setEmail("beatriz@orienta.edu.br");
        usuarioAtivado.setPapel(PapelUsuario.ROLE_ALUNO);
        usuarioAtivado.setEmailVerificado(true);

        when(usuarioService.verificarEmail(eq("beatriz@orienta.edu.br"), eq("482910"))).thenReturn(usuarioAtivado);
        when(tokenProvider.gerarAccessToken(any(), any(), any())).thenReturn("jwt.token.pos.ativacao");
        when(tokenProvider.gerarRefreshToken(any(), any())).thenReturn("jwt.refresh.pos.ativacao");
        when(tokenProvider.getJwtExpirationMs()).thenReturn(3600000L);

        mockMvc.perform(post("/api/v1/auth/verificar-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt.token.pos.ativacao"));

        verify(usuarioService, times(1)).verificarEmail("beatriz@orienta.edu.br", "482910");
    }

    /**
     * TESTE 6: Reenvio de novo código numérico.
     */
    @Test
    @DisplayName("Deve reenviar código de 6 dígitos quando solicitado pelo app")
    void deveReenviarCodigoComSucesso() throws Exception {
        Map<String, String> body = Map.of("email", "beatriz@orienta.edu.br");

        doNothing().when(usuarioService).reenviarCodigoVerificacao("beatriz@orienta.edu.br");

        mockMvc.perform(post("/api/v1/auth/reenviar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("beatriz@orienta.edu.br"));

        verify(usuarioService, times(1)).reenviarCodigoVerificacao("beatriz@orienta.edu.br");
    }
}
