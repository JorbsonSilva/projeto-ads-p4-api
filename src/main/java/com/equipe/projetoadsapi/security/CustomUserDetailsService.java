package com.equipe.projetoadsapi.security;

import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.model.PapelUsuario;
import com.equipe.projetoadsapi.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Serviço de carregamento de usuário pelo Spring Security.
 * Rastreabilidade: Fase 3 (f3-t6)
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email);
        if (usuario == null) {
            throw new UsernameNotFoundException("Usuário não encontrado com e-mail: " + email);
        }
        return new UsuarioDetails(usuario);
    }

    public UserDetails loadUserByUsernameAndRole(String email, String roleName) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email);
        if (usuario == null) {
            throw new UsernameNotFoundException("Usuário não encontrado com e-mail: " + email);
        }

        PapelUsuario papel;
        try {
            papel = PapelUsuario.valueOf(roleName);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new UsernameNotFoundException("Papel inválido no token.");
        }

        if (!usuario.possuiPapel(papel)) {
            throw new UsernameNotFoundException("O usuário não possui o papel informado no token.");
        }
        return new UsuarioDetails(usuario, papel);
    }
}
