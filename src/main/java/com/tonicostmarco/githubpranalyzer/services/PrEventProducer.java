package com.tonicostmarco.githubpranalyzer.services;

import com.tonicostmarco.githubpranalyzer.dtos.messaging.PrEventMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class PrEventProducer {


    private RabbitTemplate template;

    public PrEventProducer(RabbitTemplate template) {

        this.template = template;

    }

    public void send(PrEventMessage message) {



        template.convertAndSend("pr-exchange", "pr." + message.payload().action(), message);

    }

}




