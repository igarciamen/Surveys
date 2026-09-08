package com.igarciamen.surveys.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "survey_question_options", schema = "public")
public class QuestionOption {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private Question question;

    @NotBlank
    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "value")
    private String value;

    @Column(name = "position", nullable = false)
    private Integer position = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
}
