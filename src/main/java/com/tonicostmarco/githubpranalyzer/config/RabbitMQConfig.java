package com.tonicostmarco.githubpranalyzer.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue prEventsQueue() {
        return QueueBuilder.durable("pr-events")
                .withArgument("x-dead-letter-exchange", "pr-dead-exchange")
                .build();
    }

    @Bean
    public TopicExchange prDeadExchange() {
        return new TopicExchange("pr-dead-exchange");
    }

    @Bean
    public Queue prEventsDeadQueue() {
        return new Queue("pr-events-dlq", true);
    }

    @Bean
    public Binding deadBinding(Queue prEventsDeadQueue, TopicExchange prDeadExchange) {
        return BindingBuilder.bind(prEventsDeadQueue).to(prDeadExchange).with("#");
    }

    @Bean
    public TopicExchange prExchange() {
        return new TopicExchange("pr-exchange");
    }

    @Bean
    public Binding binding(Queue prEventsQueue, TopicExchange prExchange) {
        return BindingBuilder.bind(prEventsQueue).to(prExchange).with("pr.*");
    }

    @Bean
    public JacksonJsonMessageConverter converter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(CachingConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}

