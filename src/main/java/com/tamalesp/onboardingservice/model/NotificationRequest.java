package com.tamalesp.onboardingservice.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationRequest {

    private String recipient;
    private String tenantId;
    private String message;
    private String type;

}
