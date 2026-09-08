package com.igarciamen.responses.payloads.request;

import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;

public class SubmissionRequest {

    @Valid
    private List<AnswerInput> answers = new ArrayList<>();

    // Only used when the survey access mode is PASSWORD.
    private String accessPassword;

    public List<AnswerInput> getAnswers() { return answers; }
    public void setAnswers(List<AnswerInput> answers) {
        this.answers = (answers == null) ? new ArrayList<>() : answers;
    }

    public String getAccessPassword() { return accessPassword; }
    public void setAccessPassword(String accessPassword) { this.accessPassword = accessPassword; }
}
