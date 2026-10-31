package com.equipe.projetoadsapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CONFIGURAÇÃO GLOBAL DE CORS (Cross-Origin Resource Sharing)
 *
 * Roteiro de Defesa para a Banca Examinadora:
 * "Esta classe implementa WebMvcConfigurer para autorizar explicitamente requisições vindas
 * do ambiente móvel Expo (porta 8081, porta 19000, 3000, emuladores Android e rede local).
 * Garante que os navegadores e o motor WebView não bloqueiem preflight requests (método OPTIONS)
 * ao consumir os endpoints de autenticação e verificação de e-mail."
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "http://localhost:[*]",
                        "http://localhost:3000",
                        "http://localhost:8080",
                        "http://localhost:8081",
                        "http://localhost:19000",
                        "http://localhost:19006",
                        "http://10.0.2.2:[*]",
                        "exp://*",
                        "http://192.168.*.*",
                        "https://*"
                )
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "PATCH",
                        "OPTIONS",
                        "HEAD"
                )
                .allowedHeaders(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "X-Requested-With",
                        "Access-Control-Request-Method",
                        "Access-Control-Request-Headers"
                )
                .exposedHeaders(
                        "Authorization",
                        "Access-Control-Allow-Origin",
                        "Access-Control-Allow-Credentials"
                )
                .allowCredentials(true)
                .maxAge(3600);
    }
}
