package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.Aptidao;
import com.equipe.projetoadsapi.model.Disponibilidade;
import com.equipe.projetoadsapi.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dados profissionais permitidos na visualização pública de um professor.
 * Informações pessoais e financeiras privadas não fazem parte deste contrato.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilProfessorPublicoDTO {

    private Long id;
    private String nome;
    private String titulacao;
    private String fotoUrl;
    private Boolean verificado;
    private Boolean aceitaOnline;
    private Boolean oferecePresencial;
    private String cidade;
    private String estado;
    private String regiaoAtendimento;
    private Double notaMedia;
    private Integer totalAvaliacoes;
    private String bioProfessor;
    private BigDecimal precoHoraOnline;
    private BigDecimal precoHoraPresencial;
    private List<AptidaoDTO> aptidoes;
    private List<DisponibilidadeDTO> disponibilidades;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AptidaoDTO {
        private Long saberId;
        private String nome;
        private BigDecimal precoHora;
        private String nivelDominio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisponibilidadeDTO {
        private String diaSemana;
        private String horaInicio;
        private String horaFim;
    }

    public static PerfilProfessorPublicoDTO fromEntities(
            Usuario usuario,
            List<Aptidao> aptidoes,
            List<Disponibilidade> disponibilidades
    ) {
        List<AptidaoDTO> aptidoesDTO = aptidoes == null
                ? List.of()
                : aptidoes.stream()
                        .filter(aptidao -> aptidao.getSaber() != null)
                        .map(aptidao -> AptidaoDTO.builder()
                                .saberId(aptidao.getSaber().getId())
                                .nome(aptidao.getSaber().getNome())
                                .precoHora(aptidao.getPrecoHora())
                                .nivelDominio(aptidao.getNivelDominio())
                                .build())
                        .collect(Collectors.toList());

        List<DisponibilidadeDTO> disponibilidadesDTO = disponibilidades == null
                ? List.of()
                : disponibilidades.stream()
                        .map(disponibilidade -> DisponibilidadeDTO.builder()
                                .diaSemana(disponibilidade.getDiaSemana())
                                .horaInicio(disponibilidade.getHoraInicio() != null
                                        ? disponibilidade.getHoraInicio().toString()
                                        : null)
                                .horaFim(disponibilidade.getHoraFim() != null
                                        ? disponibilidade.getHoraFim().toString()
                                        : null)
                                .build())
                        .collect(Collectors.toList());

        return PerfilProfessorPublicoDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .titulacao(usuario.getTitulacao())
                .fotoUrl(usuario.getFotoUrl())
                .verificado(usuario.getEmailVerificado())
                .aceitaOnline(usuario.getAceitaOnline())
                .oferecePresencial(usuario.getOferecePresencial())
                .cidade(usuario.getCidade())
                .estado(usuario.getEstado())
                .regiaoAtendimento(usuario.getRegiaoAtendimento())
                .notaMedia(usuario.getNotaMedia())
                .totalAvaliacoes(usuario.getTotalAvaliacoes())
                .bioProfessor(usuario.getBioProfessor())
                .precoHoraOnline(usuario.getPrecoHoraOnline())
                .precoHoraPresencial(usuario.getPrecoHoraPresencial())
                .aptidoes(aptidoesDTO)
                .disponibilidades(disponibilidadesDTO)
                .build();
    }
}
