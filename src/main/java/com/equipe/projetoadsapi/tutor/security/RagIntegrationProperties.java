package com.equipe.projetoadsapi.tutor.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties("orienta.rag")
public record RagIntegrationProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("http://127.0.0.1:8000") URI baseUrl,
        @DefaultValue("orienta-spring") String issuer,
        @DefaultValue("orienta-rag") String audience,
        String internalSecret,
        String subjectSecret,
        @DefaultValue("60s") Duration tokenTtl,
        @DefaultValue("500ms") Duration connectTimeout,
        @DefaultValue("3s") Duration responseTimeout
) {
    @Override
    public String toString() {
        return "RagIntegrationProperties[enabled=" + enabled + "]";
    }
}
