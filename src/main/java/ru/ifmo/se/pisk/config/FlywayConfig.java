package ru.ifmo.se.pisk.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy repairAndMigrateStrategy() {
        return flyway -> {
            if (schemaHistoryTableExists(flyway)) {
                flyway.repair();
            }
            flyway.migrate();
        };
    }

    private boolean schemaHistoryTableExists(Flyway flyway) {
        DataSource dataSource = flyway.getConfiguration().getDataSource();
        if (dataSource == null) {
            return false;
        }

        String tableName = flyway.getConfiguration().getTable();

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String schema = determineSchema(flyway, connection);
            String normalizedSchema = normalizeIdentifier(schema, metaData);
            String normalizedTable = normalizeIdentifier(tableName, metaData);

            try (ResultSet tables = metaData.getTables(connection.getCatalog(), normalizedSchema, normalizedTable, null)) {
                return tables.next();
            }
        } catch (SQLException exception) {
            throw new FlywayException("Failed to inspect Flyway schema history", exception);
        }
    }

    private String determineSchema(Flyway flyway, Connection connection) throws SQLException {
        String[] schemas = flyway.getConfiguration().getSchemas();
        if (schemas.length > 0) {
            return schemas[0];
        }
        return connection.getSchema();
    }

    private String normalizeIdentifier(String identifier, DatabaseMetaData metaData) throws SQLException {
        if (identifier == null) {
            return null;
        }
        if (metaData.storesLowerCaseIdentifiers()) {
            return identifier.toLowerCase(Locale.ROOT);
        }
        if (metaData.storesUpperCaseIdentifiers()) {
            return identifier.toUpperCase(Locale.ROOT);
        }
        return identifier;
    }
}
