package com.igarciamen.responses.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "answers", schema = "public")
public class Answer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "response_id", nullable = false)
    @JsonIgnore
    private SurveyResponse response;

    // Cross-service reference to a question in surveysdb.
    @Column(name = "question_id", nullable = false)
    private Long questionId;

    // Simple value for TEXT / TEXTAREA / NUMBER / DATE / BOOLEAN.
    @Column(name = "text_value", length = 4000)
    private String textValue;

    // Chosen option ids for SINGLE_CHOICE / MULTIPLE_CHOICE / DROPDOWN / RATING.
    @ElementCollection
    @CollectionTable(
            name = "answer_selected_options",
            schema = "public",
            joinColumns = @JoinColumn(name = "answer_id")
    )
    @Column(name = "option_id")
    private List<Long> selectedOptionIds = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SurveyResponse getResponse() { return response; }
    public void setResponse(SurveyResponse response) { this.response = response; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getTextValue() { return textValue; }
    public void setTextValue(String textValue) { this.textValue = textValue; }

    public List<Long> getSelectedOptionIds() { return selectedOptionIds; }
    public void setSelectedOptionIds(List<Long> selectedOptionIds) { this.selectedOptionIds = selectedOptionIds; }
}
