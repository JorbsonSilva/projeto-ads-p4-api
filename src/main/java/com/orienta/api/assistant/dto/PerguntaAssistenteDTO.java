package com.orienta.api.assistant.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO para submissão de perguntas livres ou acionamento de chips rápidos no Assistente IA.
 * Rastreabilidade: Fase 7 - Bloco L (Tarefa [BACK-018])
 */
public class PerguntaAssistenteDTO implements Serializable {

    @NotBlank(message = "A pergunta não pode estar vazia")
    @Size(max = 1000, message = "A pergunta não pode exceder 1000 caracteres")
    @JsonAlias({"mensagem", "userMessage", "texto", "prompt"})
    private String pergunta;

    @JsonAlias({"disciplinaNome", "materia"})
    private String disciplina;

    @JsonAlias({"assuntoNome", "topico"})
    private String assunto;

    @JsonAlias({"chip", "tipoChip", "chipSelected"})
    private String chipTipo; // EXPLICAR_EXERCICIO, GERAR_RESUMO, PLANO_ESTUDO, DICAS_PROVA

    private String modalidade; // ONLINE, PRESENCIAL, AMBAS

    private BigDecimal orcamentoMaximo;

    private Long usuarioId;

    public PerguntaAssistenteDTO() {
    }

    public PerguntaAssistenteDTO(String pergunta) {
        this.pergunta = pergunta;
    }

    public PerguntaAssistenteDTO(String pergunta, String disciplina, String chipTipo) {
        this.pergunta = pergunta;
        this.disciplina = disciplina;
        this.chipTipo = chipTipo;
    }

    public String getPergunta() {
        return pergunta;
    }

    public void setPergunta(String pergunta) {
        this.pergunta = pergunta;
    }

    public String getDisciplina() {
        return disciplina;
    }

    public void setDisciplina(String disciplina) {
        this.disciplina = disciplina;
    }

    public String getAssunto() {
        return assunto;
    }

    public void setAssunto(String assunto) {
        this.assunto = assunto;
    }

    public String getChipTipo() {
        return chipTipo;
    }

    public void setChipTipo(String chipTipo) {
        this.chipTipo = chipTipo;
    }

    public String getModalidade() {
        return modalidade;
    }

    public void setModalidade(String modalidade) {
        this.modalidade = modalidade;
    }

    public BigDecimal getOrcamentoMaximo() {
        return orcamentoMaximo;
    }

    public void setOrcamentoMaximo(BigDecimal orcamentoMaximo) {
        this.orcamentoMaximo = orcamentoMaximo;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }
}
