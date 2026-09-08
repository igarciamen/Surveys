package com.igarciamen.responses.payloads.response;

import java.util.ArrayList;
import java.util.List;

public class SurveyResultsDto {
    private Long surveyId;
    private String title;
    private long totalResponses;
    private List<QuestionResultDto> questions = new ArrayList<>();

    public Long getSurveyId() { return surveyId; }
    public void setSurveyId(Long surveyId) { this.surveyId = surveyId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public long getTotalResponses() { return totalResponses; }
    public void setTotalResponses(long totalResponses) { this.totalResponses = totalResponses; }
    public List<QuestionResultDto> getQuestions() { return questions; }
    public void setQuestions(List<QuestionResultDto> questions) { this.questions = questions; }
}
