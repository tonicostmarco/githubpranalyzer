package com.tonicostmarco.githubpranalyzer.service;

import com.tonicostmarco.githubpranalyzer.dtos.messaging.PrEventMessage;
import com.tonicostmarco.githubpranalyzer.factory.PrEventMessageFactory;
import com.tonicostmarco.githubpranalyzer.repositories.PrEventRepository;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
public class PrEventConsumerDlqIT {

    @Autowired
    private RabbitTemplate template;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitListenerEndpointRegistry listenerRegistry;

    @MockitoBean
    private PrEventRepository repository;

    private PrEventMessage message;

    @BeforeEach
    void setUp() {
        rabbitAdmin.declareExchange(new TopicExchange("pr-exchange"));
        rabbitAdmin.declareQueue(QueueBuilder.durable("pr-events")
                .withArgument("x-dead-letter-exchange", "pr-dead-exchange")
                .build());
        rabbitAdmin.declareExchange(new TopicExchange("pr-dead-exchange"));
        rabbitAdmin.declareQueue(new Queue("pr-events-dlq", true));
        rabbitAdmin.declareBinding(BindingBuilder.bind(new Queue("pr-events-dlq")).to(new TopicExchange("pr-dead-exchange")).with("#"));
        rabbitAdmin.purgeQueue("pr-events-dlq");

        doThrow(new RuntimeException("erro simulado"))
                .when(repository).existsByDeliveryId(any());

        message = PrEventMessageFactory.createPrEventMessage();
        listenerRegistry.start();
    }

    @AfterEach
    void tearDown() {
        listenerRegistry.stop();
    }

    @Test
    void shouldSendToDlq_whenConsumerFailsAllRetries() {
        template.convertAndSend("pr-exchange", "pr." + message.payload().action(), message);

        await().atMost(15, TimeUnit.SECONDS)
                .until(() -> {
                    Properties props = rabbitAdmin.getQueueProperties("pr-events-dlq");
                    return props != null &&
                            (Long) props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT) > 0;
                });
    }
}
