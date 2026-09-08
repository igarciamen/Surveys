package com.igarciamen.responses.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SurveySchemaDto {
    private Long id;
    private Long ownerUserId;
    private String status;
    private String access;
    private String title;
    private List<QuestionSchemaDto> questions = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAccess() { return access; }
    public void setAccess(String access) { this.access = access; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<QuestionSchemaDto> getQuestions() { return questions; }
    public void setQuestions(List<QuestionSchemaDto> questions) { this.questions = questions; }
}
