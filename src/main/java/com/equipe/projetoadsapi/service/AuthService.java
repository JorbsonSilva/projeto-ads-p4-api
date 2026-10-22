package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.dto.AuthRequestDTO;
import com.equipe.projetoadsapi.dto.AuthResponseDTO;
import com.equipe.projetoadsapi.dto.UsuarioResponseDTO;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.PapelUsuario;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.repository.UsuarioRepository;
import com.equipe.projetoadsapi.security.JwtTokenProvider;
import com.equipe.projetoadsapi.security.LegacyPasswordMigrationService;
import com.equipe.projetoadsapi.security.RateLimitingService;
import com.equipe.projetoadsapi.security.TokenBlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Serviço de Autenticação Centralizado.
 * Rastreabilidade: Fase 3 (f3-t3, f3-t4, f3-t5, f3-t8, f3-t10)
 */
@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private TokenBlacklistService blacklistService;

    @Autowired
    private RateLimitingService rateLimitingService;

    @Autowired
    private LegacyPasswordMigrationService legacyMigrationService;

    @Autowired
    private CodigoVerificacaoService codigoService;

    @Transactional
    public AuthResponseDTO autenticar(AuthRequestDTO request) {
        rateLimitingService.verificarTentativaLogin(request.getEmail());

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail());
        if (usuario == null) {
            throw new RegraNegocioException("Credenciais inválidas: e-mail ou senha incorretos.");
        }

        boolean senhaCorreta = legacyMigrationService.verificarSenha(request.getSenha(), usuario.getSenha());
        if (!senhaCorreta) {
            throw new RegraNegocioException("Credenciais inválidas: e-mail ou senha incorretos.");
        }

        // f3-t5: Validação de e-mail verificado
        if (!Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            throw new RegraNegocioException("Seu e-mail ainda não foi verificado. Confirme o código de verificação enviado para o seu e-mail.");
        }

        // f3-t10: Atualiza senha legada para BCrypt se ainda não estiver hasheada
        legacyMigrationService.upgradeParaBcryptSeNecessario(usuario, request.getSenha());

        // Limpa tentativas de rate limiting em caso de sucesso
        rateLimitingService.limparTentativasLogin(request.getEmail());

        String papelStr = usuario.getPapel() != null ? usuario.getPapel().name() : "ROLE_ALUNO";
        String accessToken = tokenProvider.gerarAccessToken(usuario.getId(), usuario.getEmail(), papelStr);
        String refreshToken = tokenProvider.gerarRefreshToken(usuario.getId(), usuario.getEmail());

        return AuthResponseDTO.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .tipoToken("Bearer")
                .expiresIn(tokenProvider.getJwtExpirationMs() / 1000)
                .usuario(UsuarioResponseDTO.fromEntity(usuario))
                .build();
    }

    public AuthResponseDTO renovarToken(String refreshToken) {
        if (!tokenProvider.validarToken(refreshToken)) {
            throw new RegraNegocioException("Refresh token inválido ou expirado.");
        }

        String email = tokenProvider.extrairEmail(refreshToken);
        Usuario usuario = usuarioRepository.findByEmail(email);
        if (usuario == null) {
            throw new RegraNegocioException("Usuário não encontrado.");
        }

        String papelStr = usuario.getPapel() != null ? usuario.getPapel().name() : "ROLE_ALUNO";
        String novoAccessToken = tokenProvider.gerarAccessToken(usuario.getId(), usuario.getEmail(), papelStr);

        return AuthResponseDTO.builder()
                .token(novoAccessToken)
                .refreshToken(refreshToken)
                .tipoToken("Bearer")
                .expiresIn(tokenProvider.getJwtExpirationMs() / 1000)
                .usuario(UsuarioResponseDTO.fromEntity(usuario))
                .build();
    }

    public void logout(String token) {
        if (token != null) {
            Date exp = tokenProvider.extrairExpiracao(token);
            long expMillis = exp != null ? exp.getTime() : System.currentTimeMillis() + 86400000;
            blacklistService.revogarToken(token, expMillis);
        }
    }
}
