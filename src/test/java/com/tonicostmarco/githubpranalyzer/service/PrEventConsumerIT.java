package com.tonicostmarco.githubpranalyzer.service;

import com.tonicostmarco.githubpranalyzer.dtos.messaging.PrEventMessage;
import com.tonicostmarco.githubpranalyzer.entities.PrEvent;
import com.tonicostmarco.githubpranalyzer.factory.PrEventMessageFactory;
import com.tonicostmarco.githubpranalyzer.repositories.PrEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
public class PrEventConsumerIT {

    @Autowired
    private PrEventRepository repository;

    @Autowired
    private RabbitTemplate template;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitListenerEndpointRegistry listenerRegistry;

    private PrEventMessage message;

    @BeforeEach
    void setUp() {
        rabbitAdmin.declareExchange(new TopicExchange("pr-exchange"));
        rabbitAdmin.declareQueue(QueueBuilder.durable("pr-events")
                .withArgument("x-dead-letter-exchange", "pr-dead-exchange")
                .build());
        rabbitAdmin.declareBinding(BindingBuilder.bind(new Queue("pr-events"))
                .to(new TopicExchange("pr-exchange")).with("pr.*"));
        repository.deleteAll();
        message = PrEventMessageFactory.createPrEventMessage();
        listenerRegistry.start();
    }

    @AfterEach
    void tearDown() {
        listenerRegistry.stop();
    }

    @Test
    public void consume() throws InterruptedException {

        template.convertAndSend("pr-exchange", "pr." + message.payload().action(), message);

        await().atMost(5, TimeUnit.SECONDS).until(() -> repository.findByDeliveryId(message.deliveryId()).isPresent());

        Optional<PrEvent> result = repository.findByDeliveryId(message.deliveryId());

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(message.deliveryId(), result.get().getDeliveryId());
    }

}
