package com.equipe.projetoadsapi.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade de persistência de códigos de verificação de e-mail e redefinição de senha.
 * Rastreabilidade: Fase 3 (Auth & RBAC) - f3-t9
 */
@Entity
@Table(name = "tb_codigos_verificacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodigoVerificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "codigo", nullable = false, length = 10)
    private String codigo;

    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo; // CADASTRO, RECUPERACAO_SENHA

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "utilizado", nullable = false)
    private Boolean utilizado = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(expiraEm);
    }
}
