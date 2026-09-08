package com.igarciamen.statistics.client.dto;

import java.util.List;

public class QuestionResultView {
    private Long questionId;
    private String label;
    private String type;
    private long totalAnswered;
    private List<OptionCountView> options;
    private Double average;
    private Long trueCount;
    private Long falseCount;
    private List<String> samples;

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public long getTotalAnswered() { return totalAnswered; }
    public void setTotalAnswered(long totalAnswered) { this.totalAnswered = totalAnswered; }
    public List<OptionCountView> getOptions() { return options; }
    public void setOptions(List<OptionCountView> options) { this.options = options; }
    public Double getAverage() { return average; }
    public void setAverage(Double average) { this.average = average; }
    public Long getTrueCount() { return trueCount; }
    public void setTrueCount(Long trueCount) { this.trueCount = trueCount; }
    public Long getFalseCount() { return falseCount; }
    public void setFalseCount(Long falseCount) { this.falseCount = falseCount; }
    public List<String> getSamples() { return samples; }
    public void setSamples(List<String> samples) { this.samples = samples; }
}
