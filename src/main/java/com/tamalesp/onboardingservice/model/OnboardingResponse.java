package com.tamalesp.onboardingservice.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class OnboardingResponse {

    private String tenantId;
    private String status;
    private String message;
    private Instant provisionedAt;
}
