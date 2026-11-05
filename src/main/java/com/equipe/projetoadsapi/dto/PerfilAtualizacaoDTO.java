package com.equipe.projetoadsapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO UNIFICADO PARA ATUALIZAÇÃO DE PERFIL (ALUNO OU PROFESSOR).
 * Rastreabilidade: Fase 4 - f4-t3, Tarefa [BACK-004] (Requisito RN-001)
 * 
 * Suporta atualização de dados cadastrais básicos, preferências de estudo (Aluno),
 * configurações pedagógicas, valores hora/aula, raio presencial e disponibilidades (Professor).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload flexível para atualização de perfil por persona")
public class PerfilAtualizacaoDTO {

    @Schema(description = "Nome completo do usuário", example = "Carlos Silva")
    private String nome;

    @Schema(description = "Data de nascimento (YYYY-MM-DD)", example = "2000-08-25")
    private String nascimento;

    @Schema(description = "Alias para data de nascimento", example = "2000-08-25")
    private String dataNascimento;

    @Schema(description = "Biografia geral do perfil")
    private String bio;

    @Schema(description = "Biografia específica do aluno")
    private String bioAluno;

    @Schema(description = "Biografia específica do professor")
    private String bioProfessor;

    @Schema(description = "Telefone para contato", example = "(83) 98765-4321")
    private String telefone;

    @Schema(description = "Cidade", example = "João Pessoa")
    private String cidade;

    @Schema(description = "Estado (UF)", example = "PB")
    private String estado;

    @Schema(description = "URL da foto de perfil", example = "https://images.unsplash.com/photo-1534528741775-53994a69daeb")
    private String fotoUrl;

    @Schema(description = "Titulação acadêmica (perfil professor)", example = "Mestre em Ciência da Computação")
    private String titulacao;

    @Schema(description = "Preço por hora online", example = "60.00")
    private BigDecimal precoHoraOnline;

    @Schema(description = "Preço por hora presencial", example = "80.00")
    private BigDecimal precoHoraPresencial;

    @Schema(description = "Preço hora padrão", example = "60.00")
    private BigDecimal precoHora;

    @Schema(description = "Indica se oferece mentoria presencial", example = "true")
    private Boolean oferecePresencial;

    @Schema(description = "Bairro ou região de atendimento presencial", example = "Bessa / Manaíra")
    private String regiaoAtendimento;

    @Schema(description = "Raio de atendimento em km", example = "15")
    private Integer raioAtendimentoKm;

    @Schema(description = "Chave Pix para recebimento de honorários", example = "professor@orienta.com.br")
    private String chavePix;

    @Schema(description = "Tipo de chave Pix", example = "EMAIL")
    private String tipoChavePix;

    @Schema(description = "Instituição bancária para saques", example = "Banco do Brasil")
    private String banco;

    // Lista de interesses usada pelo Aluno (nomes de disciplinas)
    @Schema(description = "Lista de nomes de matérias de interesse (Aluno)")
    private List<String> interessesNomes;

    @Schema(description = "Lista de IDs de matérias de interesse (Aluno)")
    private List<Integer> interessesIds;

    // Lista de aptidões usada pelo Professor
    @Schema(description = "Lista de disciplinas com valor/hora lecionadas (Professor)")
    private List<AptidaoRequestDTO> aptidoes;

    // Grade de horários do Professor
    @Schema(description = "Grade semanal de horários disponíveis (Professor)")
    private List<DisponibilidadeRequestDTO> disponibilidades;

    public List<String> getInteresses() {
        return interessesNomes;
    }

    public void setInteresses(List<String> interesses) {
        this.interessesNomes = interesses;
    }

    public String getDataNascimentoEfetiva() {
        return dataNascimento != null && !dataNascimento.isBlank() ? dataNascimento : nascimento;
    }

    public String getBioEfetiva() {
        if (bio != null && !bio.isBlank()) return bio;
        if (bioAluno != null && !bioAluno.isBlank()) return bioAluno;
        return bioProfessor;
    }
}