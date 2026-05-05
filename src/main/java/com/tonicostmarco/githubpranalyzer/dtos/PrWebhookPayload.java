package com.tonicostmarco.githubpranalyzer.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

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
            UserData user
    ) {}

    public record RepositoryData(
            @JsonProperty("full_name") String fullName
    ) {}

    public record UserData(
            String login
    ) {}
}