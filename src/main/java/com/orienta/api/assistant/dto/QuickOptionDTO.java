package com.orienta.api.assistant.dto;

import java.io.Serializable;

/**
 * Representa um botão de opção rápida determinístico para interação no app.
 */
public class QuickOptionDTO implements Serializable {
    private String id;
    private String label;
    private String payload;
    private String description;

    public QuickOptionDTO() {
    }

    public QuickOptionDTO(String id, String label, String payload, String description) {
        this.id = id;
        this.label = label;
        this.payload = payload;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
