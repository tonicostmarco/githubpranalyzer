package com.tonicostmarco.githubpranalyzer.dtos.analytics;

public record RepositoryMetricsDTO(String repository,
                                   Long total,
                                   Long opened,
                                   Long closed,
                                   Long merged) {
}
