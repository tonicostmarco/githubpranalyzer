package com.tonicostmarco.githubpranalyzer.services;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class PrEventProducer {

        private final RabbitTemplate rabbitTemplate;

        public PrEventProducer(RabbitTemplate rabbitTemplate) {
            this.rabbitTemplate = rabbitTemplate;
        }

        public void send(String action, Object payload) {
            String routingKey = "pr." + action; // "pr.opened", "pr.closed"...
            rabbitTemplate.convertAndSend("pr-exchange", routingKey, payload);
        }

    }

