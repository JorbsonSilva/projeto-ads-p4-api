package com.equipe.projetoadsapi.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração resiliente do Flyway para ambientes de desenvolvimento e banca.
 * Executa flyway.repair() antes de flyway.migrate() para auto-recuperar checksums divergentes
 * na tabela de histórico de schema sem quebrar a inicialização da API.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
