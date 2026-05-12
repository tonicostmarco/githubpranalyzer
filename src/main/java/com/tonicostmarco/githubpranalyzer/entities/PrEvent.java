package com.tonicostmarco.githubpranalyzer.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Document(collection = "pr_events")
public class PrEvent {

        @Id
        private String id;

        private String deliveryId;
        private String action;
        private Integer prNumber;
        private String prTitle;
        private String prState;
        private Boolean merged;
        private String prAuthor;
        private String repository;
        private LocalDateTime receivedAt;
        private OffsetDateTime openedAt;
        private OffsetDateTime mergedAt;

    public PrEvent() {

    }

    public PrEvent(String deliveryId, String action, Integer prNumber, String prTitle, String prState, Boolean merged, String prAuthor, String repository, LocalDateTime receivedAt, OffsetDateTime openedAt, OffsetDateTime mergedAt) {
        this.deliveryId = deliveryId;
        this.action = action;
        this.prNumber = prNumber;
        this.prTitle = prTitle;
        this.prState = prState;
        this.merged = merged;
        this.prAuthor = prAuthor;
        this.repository = repository;
        this.receivedAt = receivedAt;
        this.openedAt = openedAt;
        this.mergedAt = mergedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getPrNumber() {
        return prNumber;
    }

    public void setPrNumber(Integer prNumber) {
        this.prNumber = prNumber;
    }

    public String getPrTitle() {
        return prTitle;
    }

    public void setPrTitle(String prTitle) {
        this.prTitle = prTitle;
    }

    public String getPrState() {
        return prState;
    }

    public void setPrState(String prState) {
        this.prState = prState;
    }

    public Boolean getMerged() {
        return merged;
    }

    public void setMerged(Boolean merged) {
        this.merged = merged;
    }

    public String getPrAuthor() {
        return prAuthor;
    }

    public void setPrAuthor(String prAuthor) {
        this.prAuthor = prAuthor;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }


    public OffsetDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(OffsetDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public OffsetDateTime getMergedAt() {
        return mergedAt;
    }

    public void setMergedAt(OffsetDateTime mergedAt) {
        this.mergedAt = mergedAt;
    }
}


