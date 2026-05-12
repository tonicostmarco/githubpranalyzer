package com.tonicostmarco.githubpranalyzer.dtos.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record PrWebhookPayload(
        String action,
        Integer number,
        @JsonProperty("pull_request") PullRequestData pullRequest,
        RepositoryData repository
) {
    public record PullRequestData(
            String title,
            String state,
            Boolean merged,
            UserData user,
            @JsonProperty("created_at") OffsetDateTime openedAt,
            @JsonProperty("merged_at") OffsetDateTime mergedAt
    ) {}

    public record RepositoryData(
            @JsonProperty("full_name") String fullName
    ) {}

    public record UserData(
            String login
    ) {}
}