package com.equipe.projetoadsapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para atualização de área de atendimento presencial e localização pelo professor.
 * Rastreabilidade: Fase 8 (f8-t1, f8-t4) / MAP-001, MAP-002, MAP-009
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtualizarLocalizacaoDTO {

    private Boolean oferecePresencial;

    @Size(max = 100, message = "A região de atendimento não pode exceder 100 caracteres")
    private String regiaoAtendimento;

    @Min(value = -90, message = "Latitude mínima é -90")
    @Max(value = 90, message = "Latitude máxima é 90")
    private Double latitudeAproximada;

    @Min(value = -180, message = "Longitude mínima é -180")
    @Max(value = 180, message = "Longitude máxima é 180")
    private Double longitudeAproximada;

    @Min(value = 1, message = "O raio mínimo de atendimento é 1 km")
    @Max(value = 50, message = "O raio máximo de atendimento é 50 km")
    private Integer raioAtendimentoKm;
}
