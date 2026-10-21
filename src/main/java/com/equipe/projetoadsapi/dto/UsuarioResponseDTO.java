package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de saída para dados de usuário.
 * Rastreabilidade: Fase 2 (Base de Qualidade e Segurança) - Tarefa f2-t4
 * REGRA CRÍTICA DE SEGURANÇA: Esta classe NUNCA possui atributo de senha ou hash,
 * impedindo o vazamento de credenciais na resposta JSON enviada aos clientes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {

    private Long id;
    private String nome;
    private String email;
    private String tipo;
    private String cpf;
    private String dataNascimento;
    private String bioAluno;
    private String bioProfessor;
    private String fotoUrl;
    private Double notaMedia;
    private Integer totalAvaliacoes;

    public static UsuarioResponseDTO fromEntity(Usuario u) {
        if (u == null) return null;
        return UsuarioResponseDTO.builder()
                .id(u.getId())
                .nome(u.getNome())
                .email(u.getEmail())
                .tipo(u.getTipo())
                .cpf(u.getCpf())
                .dataNascimento(u.getDataNascimento())
                .bioAluno(u.getBioAluno())
                .bioProfessor(u.getBioProfessor())
                .fotoUrl(u.getFotoUrl())
                .notaMedia(u.getNotaMedia())
                .totalAvaliacoes(u.getTotalAvaliacoes())
                .build();
    }
}
