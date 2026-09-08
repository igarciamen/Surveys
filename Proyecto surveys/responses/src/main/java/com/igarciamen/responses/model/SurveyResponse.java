package com.igarciamen.responses.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "survey_responses",
        schema = "public",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_response_survey_user",
                columnNames = {"survey_id", "respondent_user_id"}
        )
)
public class SurveyResponse {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // Cross-service reference (surveysdb). Plain Long, no FK across services.
    @Column(name = "survey_id", nullable = false)
    private Long surveyId;

    // Cross-service reference (usersdb). Taken from the JWT.
    @Column(name = "respondent_user_id") // nullable: anonymous submissions (OPEN/PASSWORD without login)
    private Long respondentUserId;

    @Column(name = "submitted_at", updatable = false, nullable = false)
    private Instant submittedAt;

    @OneToMany(mappedBy = "response", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.submittedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSurveyId() { return surveyId; }
    public void setSurveyId(Long surveyId) { this.surveyId = surveyId; }

    public Long getRespondentUserId() { return respondentUserId; }
    public void setRespondentUserId(Long respondentUserId) { this.respondentUserId = respondentUserId; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public List<Answer> getAnswers() { return answers; }
    public void setAnswers(List<Answer> answers) { this.answers = answers; }
}
