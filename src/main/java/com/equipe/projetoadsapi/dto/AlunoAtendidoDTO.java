package com.equipe.projetoadsapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO consolidado para a tela de Gestão de Alunos Atendidos pelo Professor ("Meus Alunos").
 * Rastreabilidade: Fase 7 - Bloco L (Tarefa [BACK-019]) / Requisito RN-001
 * 
 * Agrega métricas pedagógicas acumuladas, histórico de disciplinas e atalhos de comunicação.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlunoAtendidoDTO implements Serializable {

    // 1. Identificação do Aluno
    private Long alunoId;
    private Long id; // Compatibilidade com frontend móvel
    private String nome;
    private String fotoUrl;
    private String email;
    private String cidade;
    private String estado;
    private String curso;
    private String instituicao;

    // 2. Métricas Acumuladas de Mentoria
    @Builder.Default
    private Integer totalAulasConcluidas = 0;

    @Builder.Default
    private Integer totalAulas = 0; // Compatibilidade com frontend móvel

    @Builder.Default
    private Double horasAcumuladas = 0.0;

    @Builder.Default
    private Double horasTotais = 0.0; // Compatibilidade com frontend móvel

    private String dataUltimaAula;
    private String ultimaAulaData; // Compatibilidade com frontend móvel

    // 3. Histórico Pedagógico de Disciplinas Cursadas
    @Builder.Default
    private List<String> disciplinasCursadas = new ArrayList<>();

    @Builder.Default
    private List<String> disciplinas = new ArrayList<>(); // Compatibilidade com frontend móvel

    // 4. Atalhos de Comunicação e Navegação
    private Long agendamentoIdMaisRecente;
    private Long conversaId;

    // 5. Metadados Adicionais para a Interface Móvel
    @Builder.Default
    private String statusMentoria = "ATIVO"; // ATIVO, CONCLUIDO, AGUARDANDO_PROXIMA

    @Builder.Default
    private String formatoPreferido = "ONLINE"; // ONLINE, PRESENCIAL, HIBRIDO
}
