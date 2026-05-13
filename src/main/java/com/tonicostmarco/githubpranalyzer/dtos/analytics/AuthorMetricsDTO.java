package com.tonicostmarco.githubpranalyzer.dtos.analytics;

public record AuthorMetricsDTO(String author,
                               Long total,
                               Long merged,
                               Long opened,
                               Long closed) {
}
