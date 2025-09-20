package com.tamalesp.onboardingservice.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;

@Data
public class OnboardingRequest {

    @NotBlank
    private String tenantId;
    @NotBlank
    private String contactEmail;
    @NotBlank
    private String planType;

    private Instant provisionTime;
}
