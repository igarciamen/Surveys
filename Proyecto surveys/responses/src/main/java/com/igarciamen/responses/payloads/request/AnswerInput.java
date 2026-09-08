package com.igarciamen.responses.payloads.request;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AnswerInput {

    @NotNull
    private Long questionId;

    private String textValue;

    private List<Long> selectedOptionIds = new ArrayList<>();

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getTextValue() { return textValue; }
    public void setTextValue(String textValue) { this.textValue = textValue; }

    public List<Long> getSelectedOptionIds() { return selectedOptionIds; }
    public void setSelectedOptionIds(List<Long> selectedOptionIds) {
        this.selectedOptionIds = (selectedOptionIds == null) ? new ArrayList<>() : selectedOptionIds;
    }
}
