package com.igarciamen.statistics.service;

import com.igarciamen.statistics.client.ResponsesClient;
import com.igarciamen.statistics.client.dto.OptionCountView;
import com.igarciamen.statistics.client.dto.QuestionResultView;
import com.igarciamen.statistics.client.dto.SurveyResultsView;
import com.igarciamen.statistics.payloads.response.DailyCountDto;
import com.igarciamen.statistics.payloads.response.QuestionStatDto;
import com.igarciamen.statistics.payloads.response.StatisticsDto;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private final ResponsesClient responses;

    public StatisticsService(ResponsesClient responses) {
        this.responses = responses;
    }

    public StatisticsDto overview(Long surveyId) {
        // Both calls forward the caller's token; responses enforces owner/admin (403) and 404.
        SurveyResultsView results = responses.getResults(surveyId);
        List<Instant> timestamps = responses.getTimestamps(surveyId);

        StatisticsDto dto = new StatisticsDto();
        dto.setSurveyId(surveyId);
        dto.setTitle(results.getTitle());
        dto.setTotalResponses(results.getTotalResponses());

        // First / last response.
        if (!timestamps.isEmpty()) {
            List<Instant> sorted = timestamps.stream().sorted().toList();
            dto.setFirstResponseAt(sorted.get(0));
            dto.setLastResponseAt(sorted.get(sorted.size() - 1));
        }

        // Time series: responses grouped by day (chronological).
        Map<LocalDate, Long> byDay = timestamps.stream()
                .collect(Collectors.groupingBy(
                        ts -> ts.atZone(ZoneId.systemDefault()).toLocalDate(),
                        TreeMap::new,
                        Collectors.counting()));
        List<DailyCountDto> series = byDay.entrySet().stream()
                .map(e -> new DailyCountDto(e.getKey(), e.getValue()))
                .toList();
        dto.setResponsesByDay(series);

        // Per-question highlights.
        List<QuestionStatDto> questionStats = new ArrayList<>();
        for (QuestionResultView q : results.getQuestions()) {
            QuestionStatDto s = new QuestionStatDto();
            s.setQuestionId(q.getQuestionId());
            s.setLabel(q.getLabel());
            s.setType(q.getType());
            s.setAnswered(q.getTotalAnswered());
            s.setAnswerRate(results.getTotalResponses() == 0
                    ? 0.0
                    : round((double) q.getTotalAnswered() / results.getTotalResponses()));
            s.setHighlight(highlight(q));
            questionStats.add(s);
        }
        dto.setQuestions(questionStats);

        return dto;
    }

    private String highlight(QuestionResultView q) {
        if (q.getOptions() != null && !q.getOptions().isEmpty()) {
            OptionCountView top = q.getOptions().stream()
                    .max(Comparator.comparingLong(OptionCountView::getCount))
                    .orElse(null);
            if (top != null && top.getCount() > 0) {
                return "Most chosen: " + top.getLabel() + " (" + top.getCount() + ")";
            }
            return "No answers yet";
        }
        if (q.getAverage() != null) {
            return "Average: " + q.getAverage();
        }
        if (q.getTrueCount() != null || q.getFalseCount() != null) {
            long t = q.getTrueCount() == null ? 0 : q.getTrueCount();
            long f = q.getFalseCount() == null ? 0 : q.getFalseCount();
            return "Yes: " + t + " / No: " + f;
        }
        return q.getTotalAnswered() + " text answers";
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
