package com.tamalesp.onboardingservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingEvent {

    private String tenantId;
    private String contactEmail;
    private String planType;
    private Instant provisionTime;
}
