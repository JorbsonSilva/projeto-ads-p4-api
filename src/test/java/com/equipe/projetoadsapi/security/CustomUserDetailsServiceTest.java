package com.equipe.projetoadsapi.security;

import com.equipe.projetoadsapi.model.PapelUsuario;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void tokenRecebeSomenteOPapelAtivo() {
        Usuario usuario = usuario();
        usuario.setPapeis(Set.of(PapelUsuario.ROLE_ALUNO, PapelUsuario.ROLE_PROFESSOR));
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(usuario);

        UserDetails detalhes = userDetailsService.loadUserByUsernameAndRole(
                usuario.getEmail(), PapelUsuario.ROLE_PROFESSOR.name());

        assertEquals(
                Set.of("ROLE_PROFESSOR"),
                detalhes.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void tokenNaoAtivaPapelAindaNaoConcedido() {
        Usuario usuario = usuario();
        usuario.setPapeis(Set.of(PapelUsuario.ROLE_ALUNO));
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(usuario);

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsernameAndRole(
                        usuario.getEmail(), PapelUsuario.ROLE_PROFESSOR.name()));
    }

    private Usuario usuario() {
        return Usuario.builder()
                .id(1L)
                .nome("Conta de teste")
                .email("teste@orienta.com.br")
                .senha("hash")
                .papel(PapelUsuario.ROLE_ALUNO)
                .build();
    }
}
