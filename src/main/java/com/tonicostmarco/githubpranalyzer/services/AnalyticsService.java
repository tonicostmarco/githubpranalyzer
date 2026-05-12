package com.tonicostmarco.githubpranalyzer.services;

import com.tonicostmarco.githubpranalyzer.dtos.analytics.SummaryDTO;
import com.tonicostmarco.githubpranalyzer.entities.PrEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AnalyticsService {

    private final MongoTemplate template;

    public AnalyticsService(MongoTemplate template) {

        this.template = template;

    }

    public SummaryDTO findSummary(LocalDateTime from, LocalDateTime to) {

        MatchOperation match = Aggregation.match(Criteria.where("receivedAt").gte(from).lte(to));

        GroupOperation group = Aggregation.group()

                .count().as("total")
                .sum(ConditionalOperators.when(Criteria.where("merged")
                        .is(true)).then(1).otherwise(0))
                .as("merged")
                .sum(ConditionalOperators.when(Criteria.where("prState")
                        .is("open")).then(1).otherwise(0))
                .as("opened")
                .sum(ConditionalOperators.when(Criteria.where("prState")
                        .is("closed")).then(1).otherwise(0))
                .as("closed");


        Aggregation aggregation = Aggregation.newAggregation(match, group);


        return template.aggregate(aggregation, "pr_events", SummaryDTO.class).getUniqueMappedResult();

    }

}
