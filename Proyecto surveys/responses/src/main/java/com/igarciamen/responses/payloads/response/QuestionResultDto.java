package com.igarciamen.responses.payloads.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionResultDto {
    private Long questionId;
    private String label;
    private String type;
    private long totalAnswered;

    // For choice / dropdown / rating
    private List<OptionCountDto> options;
    // For rating / number
    private Double average;
    // For boolean
    private Long trueCount;
    private Long falseCount;
    // For free text
    private List<String> samples;

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public long getTotalAnswered() { return totalAnswered; }
    public void setTotalAnswered(long totalAnswered) { this.totalAnswered = totalAnswered; }
    public List<OptionCountDto> getOptions() { return options; }
    public void setOptions(List<OptionCountDto> options) { this.options = options; }
    public Double getAverage() { return average; }
    public void setAverage(Double average) { this.average = average; }
    public Long getTrueCount() { return trueCount; }
    public void setTrueCount(Long trueCount) { this.trueCount = trueCount; }
    public Long getFalseCount() { return falseCount; }
    public void setFalseCount(Long falseCount) { this.falseCount = falseCount; }
    public List<String> getSamples() { return samples; }
    public void setSamples(List<String> samples) { this.samples = samples; }
}
