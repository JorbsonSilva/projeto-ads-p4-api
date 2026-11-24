package com.equipe.projetoadsapi.tutor.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "orienta.rag", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RagIntegrationProperties.class)
public class RagIntegrationConfiguration {

    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");

    @Bean
    RagInternalTokenIssuer ragInternalTokenIssuer(RagIntegrationProperties properties, Environment environment) {
        validarUrl(properties.baseUrl());
        if (!Duration.ofSeconds(60).equals(properties.tokenTtl())
                || !positivo(properties.connectTimeout()) || !positivo(properties.responseTimeout())) {
            throw new IllegalArgumentException("O JWT interno exige 60 segundos de validade e timeouts positivos.");
        }
        String publicSecret = environment.getProperty("orienta.jwt.secret");
        if (publicSecret != null
                && (publicSecret.equals(properties.internalSecret()) || publicSecret.equals(properties.subjectSecret()))) {
            throw new IllegalArgumentException("Os segredos RAG devem ser exclusivos da integração.");
        }
        return new RagInternalTokenIssuer(properties, Clock.systemUTC());
    }

    private static boolean positivo(Duration duration) {
        return !duration.isZero() && !duration.isNegative();
    }

    private static void validarUrl(URI url) {
        boolean https = "https".equalsIgnoreCase(url.getScheme());
        boolean loopbackHttp = "http".equalsIgnoreCase(url.getScheme())
                && url.getHost() != null && LOOPBACK_HOSTS.contains(url.getHost().toLowerCase(Locale.ROOT));
        if ((!https && !loopbackHttp) || url.getHost() == null || url.getRawUserInfo() != null
                || url.getRawQuery() != null || url.getRawFragment() != null
                || (url.getPort() != -1 && (url.getPort() < 1 || url.getPort() > 65535))
                || (!url.getRawPath().isEmpty() && !"/".equals(url.getRawPath()))) {
            throw new IllegalArgumentException("A URL RAG deve ser uma base HTTPS ou HTTP em loopback.");
        }
    }
}
