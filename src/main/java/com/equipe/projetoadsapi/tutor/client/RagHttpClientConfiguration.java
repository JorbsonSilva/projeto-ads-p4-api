package com.equipe.projetoadsapi.tutor.client;

import com.equipe.projetoadsapi.tutor.security.RagIntegrationProperties;
import com.equipe.projetoadsapi.tutor.security.RagEnabledCondition;
import com.equipe.projetoadsapi.tutor.security.RagInternalTokenIssuer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@Conditional(RagEnabledCondition.class)
public class RagHttpClientConfiguration {

    @Bean
    RagHttpClient ragHttpClient(RagIntegrationProperties properties, RagInternalTokenIssuer issuer,
                               ObjectMapper mapper, @Qualifier("ragTransport") CloseableHttpClient transport,
                               @Value("${orienta.rag.max-concurrent-requests:4}") int maxConcurrentRequests) {
        var http = RestClient.builder().baseUrl(properties.baseUrl().toString())
                .requestFactory(new HttpComponentsClientHttpRequestFactory(transport)).build();
        return new RagHttpClient(http, issuer, mapper, maxConcurrentRequests);
    }

    @Bean(destroyMethod = "close")
    CloseableHttpClient ragTransport(RagIntegrationProperties properties,
                                     @Value("${orienta.rag.max-concurrent-requests:4}") int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("O limite de concorrência RAG deve ser positivo.");
        }
        var connect = Timeout.of(properties.connectTimeout());
        var response = Timeout.of(properties.responseTimeout());
        var connections = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(limit)
                .setMaxConnPerRoute(limit)
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(connect).setSocketTimeout(response).build())
                .build();
        return HttpClients.custom().setConnectionManager(connections)
                .setDefaultRequestConfig(RequestConfig.custom().setConnectionRequestTimeout(connect)
                        .setResponseTimeout(response).setAuthenticationEnabled(false).build())
                .disableAutomaticRetries()
                .disableRedirectHandling()
                .disableCookieManagement()
                .build();
    }
}
