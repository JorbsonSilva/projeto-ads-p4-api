package com.orienta.api.assistant.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Card de ação gerado deterministicamente pelo assistente.
 * REQUISITO DE SEGURANÇA (F9-T4 / F9-T7):
 * O assistente JAMAIS agenda automaticamente. O card fornece dados consolidados e
 * exige que o aluno clique explicitamente em "Confirmar e Revisar Horários" para navegar à tela de agendamento oficial.
 */
public class ActionCardDTO implements Serializable {
    private Long professorId;
    private String professorNome;
    private String disciplinaNome;
    private String assuntoNome;
    private String modalidade;
    private BigDecimal valorHora;
    private String targetScreenRoute;
    private String safetyNotice;
    private Boolean requiresUserConfirmation;

    public ActionCardDTO() {
        this.requiresUserConfirmation = true;
        this.safetyNotice = "Agendamento requer confirmação manual do aluno. Nenhuma cobrança ou reserva foi efetuada automaticamente.";
    }

    public ActionCardDTO(Long professorId, String professorNome, String disciplinaNome,
                         String assuntoNome, String modalidade, BigDecimal valorHora,
                         String targetScreenRoute) {
        this();
        this.professorId = professorId;
        this.professorNome = professorNome;
        this.disciplinaNome = disciplinaNome;
        this.assuntoNome = assuntoNome;
        this.modalidade = modalidade;
        this.valorHora = valorHora;
        this.targetScreenRoute = targetScreenRoute;
    }

    public Long getProfessorId() {
        return professorId;
    }

    public void setProfessorId(Long professorId) {
        this.professorId = professorId;
    }

    public String getProfessorNome() {
        return professorNome;
    }

    public void setProfessorNome(String professorNome) {
        this.professorNome = professorNome;
    }

    public String getDisciplinaNome() {
        return disciplinaNome;
    }

    public void setDisciplinaNome(String disciplinaNome) {
        this.disciplinaNome = disciplinaNome;
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

    public BigDecimal getValorHora() {
        return valorHora;
    }

    public void setValorHora(BigDecimal valorHora) {
        this.valorHora = valorHora;
    }

    public String getTargetScreenRoute() {
        return targetScreenRoute;
    }

    public void setTargetScreenRoute(String targetScreenRoute) {
        this.targetScreenRoute = targetScreenRoute;
    }

    public String getSafetyNotice() {
        return safetyNotice;
    }

    public void setSafetyNotice(String safetyNotice) {
        this.safetyNotice = safetyNotice;
    }

    public Boolean getRequiresUserConfirmation() {
        return requiresUserConfirmation;
    }

    public void setRequiresUserConfirmation(Boolean requiresUserConfirmation) {
        this.requiresUserConfirmation = requiresUserConfirmation;
    }
}
