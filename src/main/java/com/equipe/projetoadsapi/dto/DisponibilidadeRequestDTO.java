package com.equipe.projetoadsapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para envio de disponibilidades horárias semanais do professor.
 * Rastreabilidade: Tarefa [BACK-004] (Perfil Unificado)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Disponibilidade de horário semanal para mentorias")
public class DisponibilidadeRequestDTO {

    @Schema(description = "Dia da semana (ex: SEGUNDA, TERCA, QUARTA)", example = "SEGUNDA")
    private String diaSemana;

    @Schema(description = "Horário de início (HH:mm)", example = "14:00")
    private String horaInicio;

    @Schema(description = "Horário de término (HH:mm)", example = "18:00")
    private String horaFim;
}
