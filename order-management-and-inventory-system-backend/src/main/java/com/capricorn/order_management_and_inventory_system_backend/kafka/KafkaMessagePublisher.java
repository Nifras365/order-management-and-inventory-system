package com.capricorn.order_management_and_inventory_system_backend.kafka;

import com.capricorn.order_management_and_inventory_system_backend.entity.OutboxEvent;
import com.capricorn.order_management_and_inventory_system_backend.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMessagePublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC = "order-events";

    // Polls every 5 seconds
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : pendingEvents) {
            try {
                // We send the JSON payload to Kafka. The key is the aggregateId.
                kafkaTemplate.send(TOPIC, event.getAggregateId(), event.getPayload()).get();
                
                // Mark as processed if successfully sent
                event.setProcessed(true);
                outboxEventRepository.save(event);
                
                log.info("Published outbox event {} of type {} to Kafka topic {}", event.getId(), event.getType(), TOPIC);
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}", event.getId(), e);
                // The loop will continue and retry on the next scheduled run
            }
        }
    }
}
