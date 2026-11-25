package com.equipe.projetoadsapi.tutor.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RagIntegrationConfigurationTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(RagIntegrationConfiguration.class);
    private final String signingSecret = RagInternalTokenIssuerTest.secret();
    private final String subjectSecret = RagInternalTokenIssuerTest.secret();

    @Test
    void desativadoPorPadraoSemCredenciais() {
        context.run(result -> assertThat(result).hasNotFailed().doesNotHaveBean(RagInternalTokenIssuer.class));
    }

    @Test
    void ativaAutomaticamenteComOsDoisSegredos() {
        configured().run(result -> assertThat(result).hasSingleBean(RagInternalTokenIssuer.class));
    }

    @Test
    void flagFalseDesativaMesmoComOsSegredos() {
        configured().withPropertyValues("orienta.rag.enabled=false")
                .run(result -> assertThat(result).hasNotFailed().doesNotHaveBean(RagInternalTokenIssuer.class));
    }

    @Test
    void ativacaoSemCredenciaisFalha() {
        context.withPropertyValues("orienta.rag.enabled=true").run(result -> assertThat(result).hasFailed());
    }

    @Test
    void ativacaoValidaRegistraEmissor() {
        enabled().run(result -> assertThat(result).hasSingleBean(RagInternalTokenIssuer.class));
    }

    @Test
    void recusaUrlHttpForaDeLoopbackTtlIncorretoEChavePublicaReutilizada() {
        for (String property : new String[]{
                "orienta.rag.base-url=http://rag.internal:8000",
                "orienta.rag.base-url=https://rag.internal/api",
                "orienta.rag.token-ttl=61s",
                "orienta.rag.response-timeout=0ms",
                "orienta.jwt.secret=" + signingSecret,
        }) {
            enabled().withPropertyValues(property).run(result -> assertThat(result).hasFailed());
        }
    }

    private ApplicationContextRunner enabled() {
        return configured().withPropertyValues("orienta.rag.enabled=true");
    }

    private ApplicationContextRunner configured() {
        return context.withPropertyValues(
                "orienta.rag.internal-secret=" + signingSecret,
                "orienta.rag.subject-secret=" + subjectSecret);
    }
}
