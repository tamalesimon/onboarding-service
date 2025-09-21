package com.tamalesp.onboardingservice.controller;

import com.tamalesp.onboardingservice.model.OnboardingRequest;
import com.tamalesp.onboardingservice.model.OnboardingResponse;
import com.tamalesp.onboardingservice.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/onboard")
    public ResponseEntity<OnboardingResponse> onboardTenant(@Valid @RequestBody OnboardingRequest request) {
        try {
            boolean provisioned = onboardingService.provisionTenant(request);

            String message;
            HttpStatus status;

            if(provisioned) {
              message = "Tenant was provisioned successfully";
              status = HttpStatus.CREATED;
            } else{
                message = "Tenant already exists";
                status = HttpStatus.CONFLICT;
            }

//            onboardingService.provisionTenant(request);
            OnboardingResponse response = OnboardingResponse.builder()
                    .tenantId(request.getTenantId())
                    .status(status.getReasonPhrase())
                    .message(message)
                    .provisionedAt(Instant.now())
                    .build();

            return new ResponseEntity<>(response, status);
        } catch (Exception e) {
            log.error("Failed to onboard tenant.", e);
            OnboardingResponse responseFailure = OnboardingResponse.builder()
                    .tenantId(request.getTenantId())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                    .message("Failed to provision tenant: " + e.getMessage())
                    .build();

            return new ResponseEntity<>(responseFailure, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
