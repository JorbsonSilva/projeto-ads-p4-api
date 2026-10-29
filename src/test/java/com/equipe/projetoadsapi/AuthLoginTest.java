package com.equipe.projetoadsapi;

import com.equipe.projetoadsapi.dto.AuthRequestDTO;
import com.equipe.projetoadsapi.dto.AuthResponseDTO;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.PapelUsuario;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.repository.UsuarioRepository;
import com.equipe.projetoadsapi.security.JwtTokenProvider;
import com.equipe.projetoadsapi.security.LegacyPasswordMigrationService;
import com.equipe.projetoadsapi.security.RateLimitingService;
import com.equipe.projetoadsapi.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthLoginTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private RateLimitingService rateLimitingService;

    @Mock
    private LegacyPasswordMigrationService legacyMigrationService;

    @InjectMocks
    private AuthService authService;

    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        usuarioMock = Usuario.builder()
                .id(1L)
                .nome("Professor Orienta")
                .email("professor@orienta.edu.br")
                .cpf("12345678900")
                .senha("$2a$10$hashSenhaValida")
                .papel(PapelUsuario.ROLE_PROFESSOR)
                .emailVerificado(true)
                .build();
    }

    @Test
    @DisplayName("Garante login bem-sucedido quando o usuário informa seu e-mail")
    void deveAutenticarComEmail() {
        when(usuarioRepository.findByEmailOrCpf("professor@orienta.edu.br", "professor@orienta.edu.br"))
                .thenReturn(usuarioMock);
        when(legacyMigrationService.verificarSenha("senha123", usuarioMock.getSenha())).thenReturn(true);
        when(tokenProvider.gerarAccessToken(any(), anyString(), anyString())).thenReturn("jwt.token.valido");
        when(tokenProvider.gerarRefreshToken(any(), anyString())).thenReturn("refresh.token.valido");

        AuthRequestDTO request = new AuthRequestDTO("professor@orienta.edu.br", "senha123");
        AuthResponseDTO response = authService.autenticar(request);

        assertNotNull(response);
        assertEquals("jwt.token.valido", response.getToken());
        assertEquals("professor@orienta.edu.br", response.getUsuario().getEmail());
        verify(usuarioRepository, times(1)).findByEmailOrCpf("professor@orienta.edu.br", "professor@orienta.edu.br");
    }

    @Test
    @DisplayName("Garante login bem-sucedido quando o usuário informa CPF formatado (com pontos e traço)")
    void deveAutenticarComCpfFormatado() {
        when(usuarioRepository.findByEmailOrCpf("123.456.789-00", "12345678900"))
                .thenReturn(usuarioMock);
        when(legacyMigrationService.verificarSenha("senha123", usuarioMock.getSenha())).thenReturn(true);
        when(tokenProvider.gerarAccessToken(any(), anyString(), anyString())).thenReturn("jwt.token.valido");
        when(tokenProvider.gerarRefreshToken(any(), anyString())).thenReturn("refresh.token.valido");

        AuthRequestDTO request = new AuthRequestDTO("123.456.789-00", "senha123");
        AuthResponseDTO response = authService.autenticar(request);

        assertNotNull(response);
        assertEquals("jwt.token.valido", response.getToken());
        assertEquals("12345678900", response.getUsuario().getCpf());
        // Verifica que o CPF foi limpo para buscar no banco
        verify(usuarioRepository, times(1)).findByEmailOrCpf("123.456.789-00", "12345678900");
    }

    @Test
    @DisplayName("Garante que credenciais inválidas lancem RegraNegocioException")
    void deveLancarExcecaoQuandoCredenciaisInvalidas() {
        when(usuarioRepository.findByEmailOrCpf(anyString(), anyString())).thenReturn(null);

        AuthRequestDTO request = new AuthRequestDTO("usuario_inexistente@orienta.com", "senha123");

        assertThrows(RegraNegocioException.class, () -> authService.autenticar(request));
    }
}