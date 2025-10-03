package com.tamalesp.onboardingservice.service;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Service
@Slf4j
public class SchemaInitializationService {

   private final DataSource dataSource;

   public SchemaInitializationService(DataSource dataSource) {
       this.dataSource = dataSource;
   }

    public void initializeTenantSchema(String tenantId) {
        String schemaName = "tenant_" + tenantId.toLowerCase().replace('-', '_');

        try(Connection conn = dataSource.getConnection();
            Statement stmnt = conn.createStatement()) {

            // CREATE SCHEMA COMMAN
            log.info("Creating schema {} for tenant {}", schemaName, tenantId);
            stmnt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create schema " + schemaName, e);
        }

        // Apply initial table (flyway)
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/tenant")
                .load();

        log.info("Applying tenant - specific migration to schema {}", schemaName);
        flyway.migrate();
    }
}
