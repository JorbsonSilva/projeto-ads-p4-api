package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.Aptidao;
import com.equipe.projetoadsapi.model.Interesse;
import com.equipe.projetoadsapi.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO consolidado contendo perfil completo de usuário (dados cadastrais + saberes).
 * Rastreabilidade: Fase 4 - f4-t3 (Consolidar endpoints duplicados de perfil)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Perfil unificado e completo do usuário, incluindo dados acadêmicos e matérias")
public class PerfilCompletoDTO {

    @Schema(description = "ID único do usuário", example = "1")
    private Long id;

    @Schema(description = "Nome completo", example = "Carlos Silva")
    private String nome;

    @Schema(description = "E-mail de acesso institucional ou pessoal", example = "carlos@aluno.orienta.com.br")
    private String email;

    @Schema(description = "Papel no sistema (ALUNO, PROFESSOR ou ADMIN)", example = "ALUNO")
    private String tipo;

    @Schema(description = "URL da foto de perfil", example = "https://orienta.com.br/avatars/carlos.jpg")
    private String fotoUrl;

    @Schema(description = "Biografia do perfil como aluno")
    private String bioAluno;

    @Schema(description = "Biografia do perfil como professor")
    private String bioProfessor;

    @Schema(description = "Data de nascimento cadastrada", example = "2002-05-18")
    private String dataNascimento;

    @Schema(description = "Indica se o e-mail foi formalmente validado via código")
    private Boolean emailVerificado;

    // =========================================================================
    // PAGAMENTO FICTÍCIO / DEMONSTRATIVO (Decisão 18 - PROP-FIN-01)
    // =========================================================================
    @Schema(description = "Bandeira do cartão cadastrado para fins demonstrativos", example = "Mastercard")
    private String cartaoFicticioBandeira;

    @Schema(description = "Quatro últimos dígitos do cartão para exibição segura", example = "4242")
    private String cartaoFicticioUltimosDigitos;

    @Schema(description = "Nome do titular do cartão cadastrado", example = "ALUNO DEMONSTRACAO")
    private String cartaoFicticioTitular;

    @Schema(description = "Lista de matérias de interesse (perfil aluno)")
    @Builder.Default
    private List<SaberItemDTO> interesses = new ArrayList<>();

    @Schema(description = "Lista de matérias com aptidão para lecionar (perfil professor)")
    @Builder.Default
    private List<SaberItemDTO> aptidoes = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaberItemDTO {
        private Long saberId;
        private String nome;
        private String categoria;
        private String nivelOuPreco;
    }

    public static PerfilCompletoDTO fromEntities(Usuario usuario, List<Interesse> interesses, List<Aptidao> aptidoes) {
        if (usuario == null) return null;

        List<SaberItemDTO> itensInteresse = new ArrayList<>();
        if (interesses != null) {
            for (Interesse i : interesses) {
                if (i.getSaber() != null) {
                    itensInteresse.add(SaberItemDTO.builder()
                            .saberId(i.getSaber().getId())
                            .nome(i.getSaber().getNome())
                            .categoria(i.getSaber().getCategoria())
                            .nivelOuPreco(i.getNivel())
                            .build());
                }
            }
        }

        List<SaberItemDTO> itensAptidao = new ArrayList<>();
        if (aptidoes != null) {
            for (Aptidao a : aptidoes) {
                if (a.getSaber() != null) {
                    itensAptidao.add(SaberItemDTO.builder()
                            .saberId(a.getSaber().getId())
                            .nome(a.getSaber().getNome())
                            .categoria(a.getSaber().getCategoria())
                            .nivelOuPreco(a.getPreco() != null ? String.valueOf(a.getPreco()) : null)
                            .build());
                }
            }
        }

        return PerfilCompletoDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .tipo(usuario.getTipo())
                .fotoUrl(usuario.getFotoUrl())
                .bioAluno(usuario.getBioAluno())
                .bioProfessor(usuario.getBioProfessor())
                .dataNascimento(usuario.getDataNascimento())
                .emailVerificado(usuario.getEmailVerificado())
                .cartaoFicticioBandeira(usuario.getCartaoFicticioBandeira())
                .cartaoFicticioUltimosDigitos(usuario.getCartaoFicticioUltimosDigitos())
                .cartaoFicticioTitular(usuario.getCartaoFicticioTitular())
                .interesses(itensInteresse)
                .aptidoes(itensAptidao)
                .build();
    }
}
