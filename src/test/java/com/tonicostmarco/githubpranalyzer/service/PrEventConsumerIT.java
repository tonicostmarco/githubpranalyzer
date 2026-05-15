package com.tonicostmarco.githubpranalyzer.service;

import com.tonicostmarco.githubpranalyzer.dtos.messaging.PrEventMessage;
import com.tonicostmarco.githubpranalyzer.entities.PrEvent;
import com.tonicostmarco.githubpranalyzer.repositories.PrEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.Assert;

@SpringBootTest
@ActiveProfiles("test")
public class PrEventConsumerTest {

    private final PrEventRepository repository;

    public PrEventConsumerTest(PrEventRepository repository) {
        this.repository = repository;
    }

    @Test
    @RabbitListener(queues = "pr-events")
    public void consume(PrEventMessage message) {

        assertion
        if (repository.existsByDeliveryId(message.deliveryId())) {
            return;
        }

        PrEvent event = new PrEvent();

        toEntity(event, message);

        repository.save(event);
    }

}
