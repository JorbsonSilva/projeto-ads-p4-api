package com.equipe.projetoadsapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTOs de solicitação de avaliação pós-aula.
 * Rastreabilidade: Fase 6 - f6-t8 | Fase 7 - BACK-008
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para envio de avaliação e feedback pós-aula")
public class AvaliacaoRequestDTO {

    @Schema(description = "ID do usuário avaliador (opcional se fornecido via token de autenticação ou query param)", example = "1")
    private Long avaliadorId;

    @Min(value = 1, message = "A nota geral mínima é 1")
    @Max(value = 5, message = "A nota geral máxima é 5")
    @Schema(description = "Nota geral de 1 a 5 estrelas", example = "5")
    private Integer nota;

    @Min(value = 1, message = "A nota geral mínima é 1")
    @Max(value = 5, message = "A nota geral máxima é 5")
    @Schema(description = "Nota geral de 1 a 5 estrelas (alias)", example = "5")
    private Integer notaGeral;

    @Min(value = 1, message = "A pontualidade mínima é 1")
    @Max(value = 5, message = "A pontualidade máxima é 5")
    @Schema(description = "Avaliação de pontualidade de 1 a 5", example = "5")
    private Integer pontualidade;

    @Min(value = 1, message = "A pontualidade mínima é 1")
    @Max(value = 5, message = "A pontualidade máxima é 5")
    @Schema(description = "Avaliação de pontualidade de 1 a 5 (alias)", example = "5")
    private Integer notaPontualidade;

    @Min(value = 1, message = "A didática mínima é 1")
    @Max(value = 5, message = "A didática máxima é 5")
    @Schema(description = "Avaliação de didática ou clareza de 1 a 5", example = "5")
    private Integer didatica;

    @Min(value = 1, message = "A didática mínima é 1")
    @Max(value = 5, message = "A didática máxima é 5")
    @Schema(description = "Avaliação de didática ou clareza de 1 a 5 (alias)", example = "5")
    private Integer notaDidatica;

    @Min(value = 1, message = "O domínio do conteúdo mínimo é 1")
    @Max(value = 5, message = "O domínio do conteúdo máximo é 5")
    @Schema(description = "Avaliação de domínio do conteúdo de 1 a 5", example = "5")
    private Integer dominio;

    @Min(value = 1, message = "O domínio do conteúdo mínimo é 1")
    @Max(value = 5, message = "O domínio do conteúdo máximo é 5")
    @Schema(description = "Avaliação de domínio do conteúdo de 1 a 5 (alias)", example = "5")
    private Integer notaDominio;

    @Schema(description = "Tags e elogios pedagógicos selecionados", example = "[\"Excelente Didática\", \"Domínio do Conteúdo\"]")
    private List<String> tags;

    @Schema(description = "Tags e elogios pedagógicos selecionados (alias)", example = "[\"Excelente Didática\"]")
    private List<String> tagsSelecionadas;

    @Schema(description = "Comentário e observações pedagógicas sobre a aula", example = "Excelente mentoria! Explicou árvores AVL com muita clareza.")
    private String comentario;

    public Integer obterNotaFinal() {
        if (nota != null) return nota;
        if (notaGeral != null) return notaGeral;
        return null;
    }

    public Integer obterPontualidadeFinal() {
        if (pontualidade != null) return pontualidade;
        if (notaPontualidade != null) return notaPontualidade;
        return obterNotaFinal();
    }

    public Integer obterDidaticaFinal() {
        if (didatica != null) return didatica;
        if (notaDidatica != null) return notaDidatica;
        return obterNotaFinal();
    }

    public Integer obterDominioFinal() {
        if (dominio != null) return dominio;
        if (notaDominio != null) return notaDominio;
        return obterNotaFinal();
    }
}
