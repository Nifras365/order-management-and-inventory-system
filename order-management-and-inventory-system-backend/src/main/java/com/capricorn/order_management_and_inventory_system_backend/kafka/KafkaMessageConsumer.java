package com.capricorn.order_management_and_inventory_system_backend.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMessageConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-events", groupId = "order-management-group")
    public void consumeOrderEvent(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message);
            String orderId = payload.has("id") ? payload.get("id").asText() : "UNKNOWN";
            String status = payload.has("status") ? payload.get("status").asText() : "UNKNOWN";
            String email = payload.has("customerUsername") ? payload.get("customerUsername").asText() : "customer@example.com";

            log.info("--------------------------------------------------");
            log.info("📩 MOCK NOTIFICATION SYSTEM");
            log.info("Received event for Order ID: {}", orderId);
            log.info("Order Status: {}", status);
            log.info("Sending email to: {}", email);
            log.info("Email body: Dear customer, your order #{} is currently in {} status. Thank you for your business!", orderId, status);
            log.info("--------------------------------------------------");
        } catch (Exception e) {
            log.error("Failed to parse Kafka message payload", e);
        }
    }
}
