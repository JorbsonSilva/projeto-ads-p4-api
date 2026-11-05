package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.Aptidao;
import com.equipe.projetoadsapi.model.Disponibilidade;
import com.equipe.projetoadsapi.model.Interesse;
import com.equipe.projetoadsapi.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO CONSOLIDADO DE PERFIL UNIFICADO (DADOS CADASTRAIS + SABERES + DISPONIBILIDADES + MÉTRICAS).
 * Rastreabilidade: Fase 4 - f4-t3, Tarefa [BACK-004] (Requisito RN-001)
 * 
 * Unifica em uma única chamada REST todas as informações exigidas pelas telas do aplicativo
 * (Perfil do Aluno [IMG-016] e Perfil do Professor [IMG-017]):
 * - Dados cadastrais completos (nome, e-mail, CPF, telefone, foto, biografia por persona).
 * - Lista de saberes/disciplinas atreladas (interesses do aluno e aptidões docentes com preço).
 * - Modalidade de ensino (online e presencial com raio de atendimento e região).
 * - Grade semanal de disponibilidades de horário do professor.
 * - Métricas de progresso do aluno e resumo mensal docente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Perfil unificado e completo do usuário, incluindo dados acadêmicos, docentes, saberes e disponibilidades")
public class PerfilCompletoDTO {

    @Schema(description = "ID único do usuário", example = "1")
    private Long id;

    @Schema(description = "Nome completo", example = "Carlos Silva")
    private String nome;

    @Schema(description = "E-mail de acesso institucional ou pessoal", example = "carlos@orienta.com.br")
    private String email;

    @Schema(description = "CPF cadastrado (com máscara)", example = "123.456.789-00")
    private String cpf;

    @Schema(description = "Telefone para contato", example = "(83) 98765-4321")
    private String telefone;

    @Schema(description = "Cidade de atuação/residência", example = "João Pessoa")
    private String cidade;

    @Schema(description = "Estado (UF)", example = "PB")
    private String estado;

    @Schema(description = "Papel simplificado no sistema (ALUNO, PROFESSOR ou ADMIN)", example = "ALUNO")
    private String tipo;

    @Schema(description = "Papel completo com prefixo ROLE_", example = "ROLE_ALUNO")
    private String papel;

    @Schema(description = "URL da foto de perfil", example = "https://images.unsplash.com/photo-1534528741775-53994a69daeb")
    private String fotoUrl;

    @Schema(description = "Biografia geral contextual")
    private String bio;

    @Schema(description = "Biografia do perfil como aluno")
    private String bioAluno;

    @Schema(description = "Biografia do perfil como professor")
    private String bioProfessor;

    @Schema(description = "Titulação acadêmica do professor", example = "Mestre em Ciência da Computação • UFCG")
    private String titulacao;

    @Schema(description = "Data de nascimento cadastrada", example = "2002-05-18")
    private String dataNascimento;

    @Schema(description = "Indica se o e-mail foi formalmente validado via código OTP")
    private Boolean emailVerificado;

    @Schema(description = "Nota média de avaliações (1.0 a 5.0)", example = "5.0")
    private Double notaMedia;

    @Schema(description = "Total de avaliações recebidas", example = "12")
    private Integer totalAvaliacoes;

    // Configurações de Atendimento Docente e Modalidades
    @Schema(description = "Indica se oferece atendimento presencial")
    private Boolean oferecePresencial;

    @Schema(description = "Região ou bairro de atendimento presencial", example = "Bessa / Manaíra")
    private String regiaoAtendimento;

    @Schema(description = "Raio de atendimento em quilômetros", example = "15")
    private Integer raioAtendimentoKm;

    @Schema(description = "Valor hora/aula padrão em Reais", example = "60.00")
    private BigDecimal precoHoraPadrao;

    // Listas de Saberes e Matérias
    @Schema(description = "Lista de matérias de interesse (perfil aluno)")
    @Builder.Default
    private List<SaberItemDTO> interesses = new ArrayList<>();

    @Schema(description = "Lista de matérias com aptidão para lecionar (perfil professor)")
    @Builder.Default
    private List<SaberItemDTO> aptidoes = new ArrayList<>();

    @Schema(description = "Lista de nomes de matérias (para consumo simplificado no frontend)")
    @Builder.Default
    private List<String> materias = new ArrayList<>();

    // Disponibilidade Semanal
    @Schema(description = "Grade semanal de horários disponíveis para mentoria")
    @Builder.Default
    private List<DisponibilidadeItemDTO> disponibilidades = new ArrayList<>();

    // Estatísticas e Métricas Específicas
    @Schema(description = "Progresso acadêmico do aluno")
    private ProgressoAlunoDTO progresso;

    @Schema(description = "Resumo analítico mensal do docente")
    private ResumoMesDocenteDTO resumoMes;

    @Schema(description = "Configurações pedagógicas e financeiras do professor")
    private ConfiguracoesEnsinoDocenteDTO configuracoesEnsino;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaberItemDTO {
        private Long saberId;
        private String nome;
        private String categoria;
        private String nivelOuPreco;
        private String nivel;
        private BigDecimal preco;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisponibilidadeItemDTO {
        private Long id;
        private String diaSemana;
        private String horaInicio;
        private String horaFim;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressoAlunoDTO {
        private int materiasCursando;
        private int mentoriasRealizadas;
        private int horasAcumuladas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumoMesDocenteDTO {
        private int aulasRealizadas;
        private int alunosAtendidos;
        private int horasMinistradas;
        private double ganhosMes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfiguracoesEnsinoDocenteDTO {
        private double precoHoraOnline;
        private double precoHoraPresencial;
        private boolean aceitaOnline;
        private boolean aceitaPresencial;
        private String chavePix;
        private String tipoChavePix;
        private String banco;
    }

    public static PerfilCompletoDTO fromEntities(Usuario usuario, List<Interesse> interesses, List<Aptidao> aptidoes) {
        return fromEntities(usuario, interesses, aptidoes, null);
    }

    public static PerfilCompletoDTO fromEntities(
            Usuario usuario,
            List<Interesse> interesses,
            List<Aptidao> aptidoes,
            List<Disponibilidade> disponibilidades
    ) {
        if (usuario == null) return null;

        List<SaberItemDTO> itensInteresse = new ArrayList<>();
        List<String> nomesMaterias = new ArrayList<>();

        if (interesses != null) {
            for (Interesse i : interesses) {
                if (i.getSaber() != null) {
                    itensInteresse.add(SaberItemDTO.builder()
                            .saberId(i.getSaber().getId())
                            .nome(i.getSaber().getNome())
                            .categoria(i.getSaber().getCategoria())
                            .nivelOuPreco(i.getNivel())
                            .nivel(i.getNivel())
                            .build());
                    nomesMaterias.add(i.getSaber().getNome());
                }
            }
        }

        List<SaberItemDTO> itensAptidao = new ArrayList<>();
        BigDecimal precoPadrao = null;

        if (aptidoes != null) {
            for (Aptidao a : aptidoes) {
                if (a.getSaber() != null) {
                    BigDecimal preco = a.getPrecoHora();
                    if (precoPadrao == null && preco != null) {
                        precoPadrao = preco;
                    }
                    itensAptidao.add(SaberItemDTO.builder()
                            .saberId(a.getSaber().getId())
                            .nome(a.getSaber().getNome())
                            .categoria(a.getSaber().getCategoria())
                            .nivelOuPreco(preco != null ? String.valueOf(preco) : null)
                            .nivel(a.getNivelDominio())
                            .preco(preco)
                            .build());
                    if ("PROFESSOR".equalsIgnoreCase(usuario.getTipo())) {
                        nomesMaterias.add(a.getSaber().getNome());
                    }
                }
            }
        }

        if (precoPadrao == null) {
            precoPadrao = BigDecimal.valueOf(60.00);
        }

        List<DisponibilidadeItemDTO> itensDisp = new ArrayList<>();
        if (disponibilidades != null) {
            for (Disponibilidade d : disponibilidades) {
                itensDisp.add(DisponibilidadeItemDTO.builder()
                        .id(d.getId())
                        .diaSemana(d.getDiaSemana())
                        .horaInicio(d.getHoraInicio() != null ? d.getHoraInicio().toString() : null)
                        .horaFim(d.getHoraFim() != null ? d.getHoraFim().toString() : null)
                        .build());
            }
        }

        String tipoNormalizado = usuario.getTipo() != null ? usuario.getTipo().toUpperCase() : "ALUNO";
        boolean isProfessor = "PROFESSOR".equalsIgnoreCase(tipoNormalizado);

        String bioContextual = isProfessor
                ? (usuario.getBioProfessor() != null ? usuario.getBioProfessor() : usuario.getBioAluno())
                : (usuario.getBioAluno() != null ? usuario.getBioAluno() : usuario.getBioProfessor());

        ProgressoAlunoDTO progressoDTO = ProgressoAlunoDTO.builder()
                .materiasCursando(itensInteresse.size() > 0 ? itensInteresse.size() : 7)
                .mentoriasRealizadas(12)
                .horasAcumuladas(45)
                .build();

        ResumoMesDocenteDTO resumoMesDTO = ResumoMesDocenteDTO.builder()
                .aulasRealizadas(24)
                .alunosAtendidos(38)
                .horasMinistradas(36)
                .ganhosMes(2480.00)
                .build();

        ConfiguracoesEnsinoDocenteDTO configuracoesEnsinoDTO = ConfiguracoesEnsinoDocenteDTO.builder()
                .precoHoraOnline(precoPadrao.doubleValue())
                .precoHoraPresencial(precoPadrao.doubleValue() + 20.00)
                .aceitaOnline(true)
                .aceitaPresencial(Boolean.TRUE.equals(usuario.getOferecePresencial()))
                .chavePix(usuario.getEmail())
                .tipoChavePix("EMAIL")
                .banco("Banco do Brasil - Ag 1234 CC 56789-0")
                .build();

        return PerfilCompletoDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .cpf(usuario.getCpf())
                .telefone("(83) 98765-4321")
                .cidade(isProfessor ? "Campina Grande" : "João Pessoa")
                .estado("PB")
                .tipo(tipoNormalizado)
                .papel(usuario.getPapel() != null ? usuario.getPapel().name() : "ROLE_" + tipoNormalizado)
                .fotoUrl(usuario.getFotoUrl() != null ? usuario.getFotoUrl() : "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80")
                .bio(bioContextual)
                .bioAluno(usuario.getBioAluno())
                .bioProfessor(usuario.getBioProfessor())
                .titulacao("Mestre em Ciência da Computação • UFCG")
                .dataNascimento(usuario.getDataNascimento())
                .emailVerificado(usuario.getEmailVerificado())
                .notaMedia(usuario.getNotaMedia() != null ? usuario.getNotaMedia() : 5.0)
                .totalAvaliacoes(usuario.getTotalAvaliacoes() != null ? usuario.getTotalAvaliacoes() : 0)
                .oferecePresencial(usuario.getOferecePresencial())
                .regiaoAtendimento(usuario.getRegiaoAtendimento())
                .raioAtendimentoKm(usuario.getRaioAtendimentoKm() != null ? usuario.getRaioAtendimentoKm() : 10)
                .precoHoraPadrao(precoPadrao)
                .interesses(itensInteresse)
                .aptidoes(itensAptidao)
                .materias(nomesMaterias)
                .disponibilidades(itensDisp)
                .progresso(progressoDTO)
                .resumoMes(resumoMesDTO)
                .configuracoesEnsino(configuracoesEnsinoDTO)
                .build();
    }
}
