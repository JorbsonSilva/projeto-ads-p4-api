package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.Aptidao;
import com.equipe.projetoadsapi.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO DE RESULTADO DE BUSCA DE PROFESSORES COM SUPORTE A RAIO E PRESENCIAL
 * Rastreabilidade: Fase 8 (f8-t1, f8-t3, f8-t5) / MAP-005, MAP-006, LGPD MAP-SEC-002
 * 
 * Regra de Segurança: NUNCA inclui latitude e longitude do professor no JSON.
 * Exibe apenas a distância calculada pelo servidor ou o bairro/região informado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessorBuscaResponseDTO {

    private Long id;
    private String nome;
    private String bioProfessor;
    private String fotoUrl;
    private Double notaMedia;
    private Integer totalAvaliacoes;

    // Campos de Atendimento Presencial e Localização Minimizada
    private Boolean oferecePresencial;
    private String regiaoAtendimento;
    private Double distanciaAproximadaKm;
    private String textoProximidade;

    // Competências e Valores por hora
    private List<AptidaoResumoDTO> aptidoes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AptidaoResumoDTO {
        private Long saberId;
        private String saberNome;
        private Double precoHora;
        private String nivelDominio;
    }

    /**
     * Método estático para converter a entidade Usuario no DTO de Busca.
     * Aqui resolvemos a conversão de BigDecimal (Banco) para Double (JSON).
     */
    public static ProfessorBuscaResponseDTO fromEntity(Usuario usuario, Double distanciaKm, String textoProximidade) {
        
        List<AptidaoResumoDTO> aptidoesDTO = null;
        
        if (usuario.getAptidoes() != null) {
            aptidoesDTO = usuario.getAptidoes().stream().map(aptidao -> 
                AptidaoResumoDTO.builder()
                    .saberId(aptidao.getSaber() != null ? aptidao.getSaber().getId() : null)
                    .saberNome(aptidao.getSaber() != null ? aptidao.getSaber().getNome() : null)
                    // A conversão .doubleValue() abaixo resolve os erros da linha 60 e 63!
                    .precoHora(aptidao.getPrecoHora() != null ? aptidao.getPrecoHora().doubleValue() : null)
                    .nivelDominio(aptidao.getNivelDominio())
                    .build()
            ).collect(Collectors.toList());
        }

        return ProfessorBuscaResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .bioProfessor(usuario.getBioProfessor())
                .fotoUrl(usuario.getFotoUrl())
                .notaMedia(usuario.getNotaMedia())
                .totalAvaliacoes(usuario.getTotalAvaliacoes())
                .oferecePresencial(usuario.getOferecePresencial() != null && usuario.getOferecePresencial())
                .regiaoAtendimento(usuario.getRegiaoAtendimento())
                .distanciaAproximadaKm(distanciaKm)
                .textoProximidade(textoProximidade)
                .aptidoes(aptidoesDTO)
                .build();
    }
}