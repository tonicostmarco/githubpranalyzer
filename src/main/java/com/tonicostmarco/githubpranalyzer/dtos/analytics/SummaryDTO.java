package com.tonicostmarco.githubpranalyzer.dtos.analytics;

public record SummaryDTO(Long total,
                         Long merged,
                         Long opened,
                         Long closed) {
}
