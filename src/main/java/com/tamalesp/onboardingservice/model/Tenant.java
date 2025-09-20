package com.tamalesp.onboardingservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
public class Tenant {

    @Id
    private UUID id;

    private String tenantId;
    private String contactEmail;
    private String planType;
    private Instant createdAt;

}
