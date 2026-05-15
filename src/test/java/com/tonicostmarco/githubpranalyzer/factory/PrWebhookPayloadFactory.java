package com.tonicostmarco.githubpranalyzer.factory;

import com.tonicostmarco.githubpranalyzer.dtos.messaging.PrWebhookPayload;
import com.tonicostmarco.githubpranalyzer.entities.PrEvent;

public class PrWebhookPayloadFactory {

    public static PrWebhookPayload createPayload() {

        PrEvent pr = PrEventFactory.createPrEvent();

        PrWebhookPayload.UserData uD = new PrWebhookPayload.UserData(pr.getPrAuthor());
        PrWebhookPayload.PullRequestData prD = new PrWebhookPayload.PullRequestData(pr.getPrTitle(), pr.getPrState(), pr.getMerged(), uD, pr.getOpenedAt(), pr.getMergedAt());
        PrWebhookPayload.RepositoryData rD = new PrWebhookPayload.RepositoryData(pr.getRepository());

        PrWebhookPayload p = new PrWebhookPayload(pr.getAction(), pr.getPrNumber(), prD, rD);
        return p;
    }

}
