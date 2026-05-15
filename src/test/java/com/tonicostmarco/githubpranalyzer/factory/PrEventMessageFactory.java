package com.tonicostmarco.githubpranalyzer.factory;

import com.tonicostmarco.githubpranalyzer.dtos.messaging.PrEventMessage;

public class PrEventMessageFactory {

    public static PrEventMessage createPrEventMessage() {

        return new PrEventMessage(PrEventFactory.createPrEvent().getDeliveryId(), PrWebhookPayloadFactory.createPayload());

    }

}
