package com.equipe.projetoadsapi.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração OpenAPI 3.0 / Swagger UI para a API Orienta (v1).
 * Rastreabilidade: Fase 4 (Padronizar Contrato Mobile/API) - Tarefa f4-t5
 * 
 * Disponibiliza documentação viva interativa com esquema de autenticação JWT Bearer.
 * Endpoints:
 * - Swagger UI: /api/v1/swagger-ui.html e /swagger-ui.html
 * - OpenAPI JSON: /api/v1/api-docs e /v3/api-docs
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Plataforma Orienta — API RESTful (P4)",
        version = "v1.0.0",
        description = "Documentação oficial e interativa dos contratos de API do Sistema Orienta. " +
                      "Arquitetura de Monólito Modular (Spring Boot 3.4.5, Java 17, PostgreSQL, JWT). " +
                      "Atende ao aplicativo móvel React Native e interfaces web administrativas a custo zero.",
        contact = @Contact(
            name = "Equipe ADS - P4 (Orienta)",
            email = "orienta.projeto.ads@gmail.com",
            url = "https://github.com/JorbsonSilva/projeto-ads-p4-api"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(url = "/", description = "Servidor Local / Atual (Porta 8080)"),
        @Server(url = "http://localhost:8080", description = "Ambiente de Desenvolvimento Local")
    },
    security = {
        @SecurityRequirement(name = "bearerAuth")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    description = "Autenticação via JSON Web Token (JWT). Insira o token retornado na rota /api/v1/auth/login.",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT"
)
public class OpenApiConfig {
}
