package com.equipe.projetoadsapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * CONFIGURAÇÕES GERAIS DE APLICAÇÃO
 * Rastreabilidade: Fase 2 (Base de Qualidade e Segurança) - f2-t7
 * Clock injetável para permitir testes determinísticos de regras de tempo (ex: antecedência de 24h).
 */
@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
