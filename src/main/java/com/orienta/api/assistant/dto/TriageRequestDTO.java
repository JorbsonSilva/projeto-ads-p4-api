package com.orienta.api.assistant.dto;

import com.orienta.api.assistant.model.TriageStep;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Dados de entrada enviados pelo app mobile para avanço determinístico na triagem.
 * Atende ao requisito F9-T2 com validação rigorosa de dados.
 */
public class TriageRequestDTO implements Serializable {

    private TriageStep currentStep;

    @Size(max = 250, message = "Mensagem não pode exceder 250 caracteres")
    private String userMessage;

    private Long disciplinaId;

    @Size(max = 100, message = "Nome da disciplina inválido")
    private String disciplinaNome;

    private Long assuntoId;

    @Size(max = 100, message = "Nome do assunto inválido")
    private String assuntoNome;

    private String modalidade; // ONLINE, PRESENCIAL, AMBAS

    @DecimalMin(value = "10.00", message = "O orçamento mínimo aceito é de R$ 10,00/h")
    @DecimalMax(value = "500.00", message = "O orçamento máximo aceito é de R$ 500,00/h")
    private BigDecimal orcamentoMaximo;

    private String bairroFiltro;

    private String selectedOptionPayload;

    public TriageRequestDTO() {
    }

    public TriageStep getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(TriageStep currentStep) {
        this.currentStep = currentStep;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public Long getDisciplinaId() {
        return disciplinaId;
    }

    public void setDisciplinaId(Long disciplinaId) {
        this.disciplinaId = disciplinaId;
    }

    public String getDisciplinaNome() {
        return disciplinaNome;
    }

    public void setDisciplinaNome(String disciplinaNome) {
        this.disciplinaNome = disciplinaNome;
    }

    public Long getAssuntoId() {
        return assuntoId;
    }

    public void setAssuntoId(Long assuntoId) {
        this.assuntoId = assuntoId;
    }

    public String getAssuntoNome() {
        return assuntoNome;
    }

    public void setAssuntoNome(String assuntoNome) {
        this.assuntoNome = assuntoNome;
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

    public String getBairroFiltro() {
        return bairroFiltro;
    }

    public void setBairroFiltro(String bairroFiltro) {
        this.bairroFiltro = bairroFiltro;
    }

    public String getSelectedOptionPayload() {
        return selectedOptionPayload;
    }

    public void setSelectedOptionPayload(String selectedOptionPayload) {
        this.selectedOptionPayload = selectedOptionPayload;
    }
}
