package ru.ifmo.se.pisk.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // Отключаем валидацию перед миграцией
            flyway.setValidateOnMigrate(false);
            flyway.setValidateMigrationNaming(false);
            // Выполняем миграцию
            flyway.migrate();
        };
    }
}
