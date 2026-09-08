package com.igarciamen.statistics.payloads.response;

public class QuestionStatDto {
    private Long questionId;
    private String label;
    private String type;
    private long answered;
    private double answerRate;   // answered / totalResponses (0..1)
    private String highlight;    // a short human-readable summary

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public long getAnswered() { return answered; }
    public void setAnswered(long answered) { this.answered = answered; }
    public double getAnswerRate() { return answerRate; }
    public void setAnswerRate(double answerRate) { this.answerRate = answerRate; }
    public String getHighlight() { return highlight; }
    public void setHighlight(String highlight) { this.highlight = highlight; }
}
