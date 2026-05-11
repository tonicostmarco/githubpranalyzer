package com.tonicostmarco.githubpranalyzer.controller;

import com.tonicostmarco.githubpranalyzer.dtos.PrEventMessage;
import com.tonicostmarco.githubpranalyzer.dtos.PrWebhookPayload;
import com.tonicostmarco.githubpranalyzer.services.PrEventProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/webhook/notify")
public class PrEventController {

    private final PrEventProducer producer;

    public PrEventController(PrEventProducer producer) {
        this.producer = producer;
    }

    @PostMapping

    public ResponseEntity<PrEventMessage> webhookReceiver(@RequestHeader("X-GitHub-Delivery") String deliveryId, @RequestBody PrWebhookPayload payload) {



        PrEventMessage eventMessage = new PrEventMessage(deliveryId, payload);

        producer.send(eventMessage);

        return ResponseEntity.ok().build();
    }

}
