package com.capricorn.order_management_and_inventory_system_backend.service;

import com.capricorn.order_management_and_inventory_system_backend.entity.OutboxEvent;
import com.capricorn.order_management_and_inventory_system_backend.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public void saveEvent(String aggregateType, String aggregateId, String type, Object payload) {
        String payloadJson = objectMapper.writeValueAsString(payload);

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .type(type)
                .payload(payloadJson)
                .processed(false)
                .build();

        outboxEventRepository.save(event);
    }
}
