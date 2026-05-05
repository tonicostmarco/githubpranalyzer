package com.tonicostmarco.githubpranalyzer.dtos;

public record PrEventMessage(
        String deliveryId,
        PrWebhookPayload payload
) {}
