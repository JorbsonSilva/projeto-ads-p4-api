package com.orienta.api.assistant.dto;

import com.orienta.api.assistant.model.TriageStep;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Resposta determinística retornada para o aplicativo cliente.
 * Não depende de provedores pagos e não gera saídas geradas por LLM.
 */
public class TriageResponseDTO implements Serializable {

    private String assistantMessage;
    private TriageStep nextStep;
    private List<QuickOptionDTO> quickOptions = new ArrayList<>();
    private List<ProfessorResumoDTO> recommendedTeachers = new ArrayList<>();
    private ActionCardDTO actionCard;
    private String supportTicketDraftProtocol;
    private boolean isDeterministicRuleBased = true;
    private String ruleReferenceCode; // Ex: RULE_TREE_V9_01

    public TriageResponseDTO() {
    }

    public static TriageResponseDTO create(String message, TriageStep nextStep, String ruleCode) {
        TriageResponseDTO dto = new TriageResponseDTO();
        dto.setAssistantMessage(message);
        dto.setNextStep(nextStep);
        dto.setRuleReferenceCode(ruleCode);
        return dto;
    }

    public String getAssistantMessage() {
        return assistantMessage;
    }

    public void setAssistantMessage(String assistantMessage) {
        this.assistantMessage = assistantMessage;
    }

    public TriageStep getNextStep() {
        return nextStep;
    }

    public void setNextStep(TriageStep nextStep) {
        this.nextStep = nextStep;
    }

    public List<QuickOptionDTO> getQuickOptions() {
        return quickOptions;
    }

    public void setQuickOptions(List<QuickOptionDTO> quickOptions) {
        this.quickOptions = quickOptions;
    }

    public List<ProfessorResumoDTO> getRecommendedTeachers() {
        return recommendedTeachers;
    }

    public void setRecommendedTeachers(List<ProfessorResumoDTO> recommendedTeachers) {
        this.recommendedTeachers = recommendedTeachers;
    }

    public ActionCardDTO getActionCard() {
        return actionCard;
    }

    public void setActionCard(ActionCardDTO actionCard) {
        this.actionCard = actionCard;
    }

    public String getSupportTicketDraftProtocol() {
        return supportTicketDraftProtocol;
    }

    public void setSupportTicketDraftProtocol(String supportTicketDraftProtocol) {
        this.supportTicketDraftProtocol = supportTicketDraftProtocol;
    }

    public boolean isDeterministicRuleBased() {
        return isDeterministicRuleBased;
    }

    public void setDeterministicRuleBased(boolean deterministicRuleBased) {
        isDeterministicRuleBased = deterministicRuleBased;
    }

    public String getRuleReferenceCode() {
        return ruleReferenceCode;
    }

    public void setRuleReferenceCode(String ruleReferenceCode) {
        this.ruleReferenceCode = ruleReferenceCode;
    }
}
