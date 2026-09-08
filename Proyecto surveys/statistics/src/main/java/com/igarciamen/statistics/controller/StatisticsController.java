package com.igarciamen.statistics.controller;

import com.igarciamen.statistics.payloads.response.StatisticsDto;
import com.igarciamen.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService service;

    public StatisticsController(StatisticsService service) {
        this.service = service;
    }

    @Operation(summary = "Statistics overview of a survey (owner or admin only)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(path = "/survey/{surveyId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public StatisticsDto overview(@PathVariable Long surveyId) {
        return service.overview(surveyId);
    }
}
