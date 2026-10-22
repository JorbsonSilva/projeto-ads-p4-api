package com.equipe.projetoadsapi.security;

import com.equipe.projetoadsapi.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Implementação de UserDetails adaptando Usuario para o Spring Security.
 * Rastreabilidade: Fase 3 (f3-t6)
 */
public class UsuarioDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String senha;
    private final boolean emailVerificado;
    private final Collection<? extends GrantedAuthority> authorities;

    public UsuarioDetails(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.senha = usuario.getSenha();
        this.emailVerificado = Boolean.TRUE.equals(usuario.getEmailVerificado());
        String papelNome = usuario.getPapel() != null ? usuario.getPapel().name() : "ROLE_ALUNO";
        this.authorities = List.of(new SimpleGrantedAuthority(papelNome));
    }

    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return emailVerificado;
    }
}
