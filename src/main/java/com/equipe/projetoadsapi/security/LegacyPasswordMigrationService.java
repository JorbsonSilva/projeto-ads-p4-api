package com.equipe.projetoadsapi.security;

import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de upgrade transparente de senhas legadas em texto puro para BCrypt.
 * Rastreabilidade: Fase 3 (f3-t10)
 * Quando um usuário legado faz login com sucesso usando sua senha antiga,
 * sua senha é automaticamente re-criptografada para BCrypt no banco.
 */
@Service
public class LegacyPasswordMigrationService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public boolean isBcryptHash(String senha) {
        if (senha == null) return false;
        return senha.startsWith("$2a$") || senha.startsWith("$2b$") || senha.startsWith("$2y$");
    }

    public boolean verificarSenha(String senhaDigitada, String senhaArmazenada) {
        if (isBcryptHash(senhaArmazenada)) {
            return passwordEncoder.matches(senhaDigitada, senhaArmazenada);
        } else {
            // Senha legada em texto puro
            return senhaArmazenada.equals(senhaDigitada);
        }
    }

    @Transactional
    public void upgradeParaBcryptSeNecessario(Usuario usuario, String senhaDigitada) {
        if (!isBcryptHash(usuario.getSenha())) {
            usuario.setSenha(passwordEncoder.encode(senhaDigitada));
            usuarioRepository.save(usuario);
        }
    }
}
