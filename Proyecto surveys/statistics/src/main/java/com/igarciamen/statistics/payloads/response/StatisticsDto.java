package com.igarciamen.statistics.payloads.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatisticsDto {
    private Long surveyId;
    private String title;
    private long totalResponses;
    private Instant firstResponseAt;
    private Instant lastResponseAt;
    private List<DailyCountDto> responsesByDay = new ArrayList<>();
    private List<QuestionStatDto> questions = new ArrayList<>();

    public Long getSurveyId() { return surveyId; }
    public void setSurveyId(Long surveyId) { this.surveyId = surveyId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public long getTotalResponses() { return totalResponses; }
    public void setTotalResponses(long totalResponses) { this.totalResponses = totalResponses; }
    public Instant getFirstResponseAt() { return firstResponseAt; }
    public void setFirstResponseAt(Instant firstResponseAt) { this.firstResponseAt = firstResponseAt; }
    public Instant getLastResponseAt() { return lastResponseAt; }
    public void setLastResponseAt(Instant lastResponseAt) { this.lastResponseAt = lastResponseAt; }
    public List<DailyCountDto> getResponsesByDay() { return responsesByDay; }
    public void setResponsesByDay(List<DailyCountDto> responsesByDay) { this.responsesByDay = responsesByDay; }
    public List<QuestionStatDto> getQuestions() { return questions; }
    public void setQuestions(List<QuestionStatDto> questions) { this.questions = questions; }
}
