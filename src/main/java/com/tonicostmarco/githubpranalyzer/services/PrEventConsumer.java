package com.tonicostmarco.githubpranalyzer.services;

import com.tonicostmarco.githubpranalyzer.dtos.messaging.PrEventMessage;
import com.tonicostmarco.githubpranalyzer.entities.PrEvent;
import com.tonicostmarco.githubpranalyzer.repositories.PrEventRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PrEventConsumer {

    private final PrEventRepository repository;

    public PrEventConsumer(PrEventRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = "pr-events")
    public void consume(PrEventMessage message) {

        if (repository.existsByDeliveryId(message.deliveryId())) {
            return;
        }

        PrEvent event = new PrEvent();

        toEntity(event, message);

        repository.save(event);
    }


    public void toEntity(PrEvent event, PrEventMessage message) {

        event.setDeliveryId(message.deliveryId());
        event.setAction(message.payload().action());
        event.setPrNumber(message.payload().number());
        event.setPrTitle(message.payload().pullRequest().title());
        event.setPrState(message.payload().pullRequest().state());
        event.setMerged(message.payload().pullRequest().merged());
        event.setPrAuthor(message.payload().pullRequest().user().login());
        event.setRepository(message.payload().repository().fullName());
        event.setReceivedAt(LocalDateTime.now());
        event.setOpenedAt(message.payload().pullRequest().openedAt());
        event.setMergedAt(message.payload().pullRequest().mergedAt());
    }
}

