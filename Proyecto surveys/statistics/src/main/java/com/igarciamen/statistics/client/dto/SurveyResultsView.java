package com.igarciamen.statistics.client.dto;

import java.util.ArrayList;
import java.util.List;

public class SurveyResultsView {
    private Long surveyId;
    private String title;
    private long totalResponses;
    private List<QuestionResultView> questions = new ArrayList<>();

    public Long getSurveyId() { return surveyId; }
    public void setSurveyId(Long surveyId) { this.surveyId = surveyId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public long getTotalResponses() { return totalResponses; }
    public void setTotalResponses(long totalResponses) { this.totalResponses = totalResponses; }
    public List<QuestionResultView> getQuestions() { return questions; }
    public void setQuestions(List<QuestionResultView> questions) { this.questions = questions; }
}
