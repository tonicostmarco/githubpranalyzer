package com.tonicostmarco.githubpranalyzer.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

        @Bean
        public Queue prEventsQueue() {
            return new Queue("pr-events", true);
        }

        @Bean
        public TopicExchange prExchange() {
            return new TopicExchange("pr-exchange");
        }
        @Bean
        public Binding binding(Queue prEventsQueue, TopicExchange prExchange) {
            return BindingBuilder.bind(prEventsQueue).to(prExchange).with("pr.*");
        }
    }

