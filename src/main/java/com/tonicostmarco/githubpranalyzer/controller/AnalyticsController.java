package com.tonicostmarco.githubpranalyzer.controller;

import com.tonicostmarco.githubpranalyzer.dtos.analytics.AuthorMetricsDTO;
import com.tonicostmarco.githubpranalyzer.dtos.analytics.SummaryDTO;
import com.tonicostmarco.githubpranalyzer.services.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {


    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping(value = "/summary")
    public ResponseEntity<SummaryDTO> findSummary(@RequestParam(name = "from") LocalDateTime from, @RequestParam(name = "to")LocalDateTime to) {

        SummaryDTO dto = service.findSummary(from, to);

        return ResponseEntity.ok(dto);

    }

    @GetMapping(value = "/authormetrics")
    public ResponseEntity<List<AuthorMetricsDTO>> findAuthorMetrics(@RequestParam(name = "from") LocalDateTime from, @RequestParam(name = "to") LocalDateTime to) {

        List<AuthorMetricsDTO> dto = service.findAuthorMetrics(from, to);

        return ResponseEntity.ok(dto);

    }

}
