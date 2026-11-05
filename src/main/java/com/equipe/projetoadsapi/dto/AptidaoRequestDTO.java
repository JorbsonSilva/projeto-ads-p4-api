package com.equipe.projetoadsapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO DE REQUISIÇÃO PARA APTIDÕES DOCENTES
 * Rastreabilidade: Tarefas [BACK-003], [BACK-004]
 * 
 * Captura matéria lecionada, nível de domínio e valor por hora-aula (BigDecimal).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados de aptidão e disciplina lecionada pelo professor")
public class AptidaoRequestDTO {

    @Schema(description = "ID da matéria no catálogo (opcional se informado o nome)", example = "1")
    private Long saberId;

    @Schema(description = "Nome da matéria ou disciplina", example = "Cálculo I")
    private String nomeSaber;

    @Schema(description = "Nível de domínio (Iniciante, Intermediário, Avançado)", example = "Avançado")
    private String nivelDominio;

    @Schema(description = "Preço cobrado por hora de mentoria (R$)", example = "60.00")
    private BigDecimal precoHora;

    public String getMateria() {
        return nomeSaber;
    }

    public void setMateria(String materia) {
        this.nomeSaber = materia;
    }

    public BigDecimal getPreco() {
        return precoHora;
    }

    public void setPreco(BigDecimal preco) {
        this.precoHora = preco;
    }
}