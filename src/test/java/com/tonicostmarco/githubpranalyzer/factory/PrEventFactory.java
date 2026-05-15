package com.tonicostmarco.githubpranalyzer.factory;

import com.tonicostmarco.githubpranalyzer.entities.PrEvent;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class PrEventFactory {

    public static PrEvent createPrEvent() {

        String id = "testeid123";
        String deliveryId = "testedId123";
        String action = "opened";
        Integer prNumber = 42;
        String prTitle = "Fix bug no login";
        String prState = "open";
        Boolean merged = false;
        String prAuthor = "tonicostmarco";
        String repository = "tonicostmarco/github-pr-analyzer";
        LocalDateTime receivedAt = LocalDateTime.now();
        OffsetDateTime openedAt = OffsetDateTime.now();
        OffsetDateTime mergedAt = OffsetDateTime.now().plusSeconds(5667);

        PrEvent pr = new PrEvent(deliveryId, action, prNumber, prTitle, prState, merged, prAuthor, repository, receivedAt, openedAt, mergedAt) ;

        return pr;
    }

}
