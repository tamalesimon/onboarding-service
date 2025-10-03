package com.tamalesp.onboardingservice.kafka;

import com.tamalesp.onboardingservice.model.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationProducer {

    private static final String NOTIFICATION_TOPIC = "notifications";
    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    public NotificationProducer(KafkaTemplate<String, NotificationRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendSuccessOnboardingNotification(String recipient, String tenantId) {

        String message = String.format(
                "Welcome to Qube!\n\nYour tenant ID: %s",
                tenantId
        );

            // Build the specific notification request
        NotificationRequest welcomeEmail = NotificationRequest.builder()
                .type("email")
                .recipient(recipient)
                .tenantId(tenantId)
                .message(message)
                .build();

        // Publish the request to kafka
        kafkaTemplate.send(NOTIFICATION_TOPIC, tenantId, welcomeEmail)
                .whenComplete((notificationResponse, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to send notification", throwable);
                        throwable.printStackTrace();
                    } else {
                        log.info("Notification Sent Successfully");
                    }
                });
    }
}
