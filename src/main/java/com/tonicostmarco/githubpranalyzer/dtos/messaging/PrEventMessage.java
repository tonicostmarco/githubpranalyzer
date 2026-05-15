package com.tonicostmarco.githubpranalyzer.dtos.messaging;

public record PrEventMessage(
        String deliveryId,
        PrWebhookPayload payload
) {

}
