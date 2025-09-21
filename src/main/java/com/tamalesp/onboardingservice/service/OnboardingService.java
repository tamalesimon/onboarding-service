package com.tamalesp.onboardingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamalesp.onboardingservice.model.OnboardingEvent;
import com.tamalesp.onboardingservice.model.OnboardingRequest;
import com.tamalesp.onboardingservice.model.Tenant;
import com.tamalesp.onboardingservice.repository.TenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;

import org.apache.kafka.common.quota.ClientQuotaAlteration;
import org.apache.kafka.common.quota.ClientQuotaEntity;
import org.flywaydb.core.Flyway;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class OnboardingService {

    private final TenantRepository tenantRepository;
    private final KafkaAdmin kafkaAdmin;
    private final DataSource dataSource;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;


    public OnboardingService(TenantRepository tenantRepository, KafkaAdmin kafkaAdmin, DataSource dataSource, KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.kafkaAdmin = kafkaAdmin;
        this.dataSource = dataSource;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public boolean provisionTenant(OnboardingRequest request) {
        // 1. Check for idempotency: if tenant already exists, do nothing
        if(tenantRepository.findByTenantId(request.getTenantId()).isPresent()){
            log.info("Tenant {} already exists", request.getTenantId());
            return false;
        }
        Instant provisionTime = Instant.now();

        // 2. Provision tenant in the database
        Tenant newTenant = new Tenant();
        newTenant.setId(UUID.randomUUID());
        newTenant.setTenantId(request.getTenantId());
        newTenant.setContactEmail(request.getContactEmail());
        newTenant.setPlanType(request.getPlanType());
        newTenant.setCreatedAt(provisionTime);

        try{
            tenantRepository.save(newTenant);
        } catch (DataIntegrityViolationException e) {
            // Catch this specifically for duplicate tenantId
            log.warn("Race conditioning detected on Tenant {}", request.getTenantId());
            if(tenantRepository.findByTenantId(request.getTenantId()).isPresent()) {
                // Another thread may have just created it. Handle gracefully.
                return false;
            }
            throw new RuntimeException("Failed to save the tenant. ",e);
        }

        // 3. Provision Kafka topic
        String topicName = "logs."+ request.getTenantId();
        NewTopic newKafkaTopic = new NewTopic(topicName, 3, (short) 1);
        kafkaAdmin.createOrModifyTopics(newKafkaTopic);
        applyKafkaQuota((request.getTenantId()));

        // 4. Provision PostgresSQL schema (Not directly supported by JPA, requires a separate JDBC call)
        // Provision PostgresSQL schema using Flyway
        createTenantSchema(request.getTenantId());

        // 5. Trigger other actions, e.g., billing service and notification to notify the tenant that provision is completed call
        // through publishing an event which will be consumed;

        OnboardingEvent event = new OnboardingEvent();
        event.setTenantId(request.getTenantId());
        event.setContactEmail(request.getContactEmail());
        event.setPlanType(request.getPlanType());
        event.setProvisionTime(provisionTime);

        try {
            String jsonEvent = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("tenant-onboarding-events", request.getTenantId(), jsonEvent);
            log.info("Published onboarding event for tenant {}", request.getTenantId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OnboardingEvent", e);
            throw new RuntimeException("Failed to serialize onboarding event", e);
        }

        return true;
    }

    public void createTenantSchema(String tenantId){
        // Implementation of schema creation will go here. This requires a native SQL query.
        // For a true production-ready service, we would use a library like Flyway or Liquibase for schema management.
        String schemaName = "schema_" + tenantId.toLowerCase().replaceAll("-", "_");

        // Using flyway to create the schema and run migration
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration/tenant")
                .load();

        flyway.migrate();
        log.info("Successfully provisioned a database schema for {} using flyway.", tenantId);
    }

    private void applyKafkaQuota(String tenantId){
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            String clientId = "tenant_" + tenantId;
            ClientQuotaAlteration alteration = getAlteration(clientId);

            // Apply the quota using alterClientQuotas
            AlterClientQuotasResult result = adminClient.alterClientQuotas(Collections.singletonList(alteration));

            result.all().get();
            log.info("Applied Kafka quotas for client ID: {}", clientId);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to apply Kafka quotas for tenant {}: {}", tenantId, e.getMessage());
            Thread.currentThread().interrupt();
            throw new RuntimeException("Kafka quota application failed.", e);
        }
    }

    private static ClientQuotaAlteration getAlteration(String clientId) {
        Map<String, String> entityMap = Collections.singletonMap(ClientQuotaEntity.CLIENT_ID, clientId);
        ClientQuotaEntity quotaEntity = new ClientQuotaEntity(entityMap);

        // Create the quota operations
        ClientQuotaAlteration.Op producerQuotaOp = new ClientQuotaAlteration.Op("producer_byte_rate", 10485760.0); // 10 MB/s
        ClientQuotaAlteration.Op consumerQuotaOp = new ClientQuotaAlteration.Op("consumer_byte_rate", 20971520.0); // 20 MB/s

        // Create the alteration object
        return new ClientQuotaAlteration(
                quotaEntity,
                Arrays.asList(producerQuotaOp, consumerQuotaOp));
    }
}
