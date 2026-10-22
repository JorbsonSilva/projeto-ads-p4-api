package com.equipe.projetoadsapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO para remarcação de horário de aula.
 * Rastreabilidade: Fase 4 - f4-t6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para solicitar reagendamento de uma aula")
public class RemarcarAgendamentoDTO {

    @NotNull(message = "A nova data da aula é obrigatória")
    @FutureOrPresent(message = "A data deve ser hoje ou uma data futura")
    @Schema(description = "Nova data desejada (YYYY-MM-DD)", example = "2026-11-20")
    private LocalDate data;

    @NotNull(message = "O novo horário é obrigatório")
    @Schema(description = "Novo horário desejado (HH:mm:ss)", example = "16:00:00")
    private LocalTime hora;

    @Schema(description = "Motivo da alteração de data/horário", example = "Conflito com prova presencial na faculdade")
    private String motivo;
}
