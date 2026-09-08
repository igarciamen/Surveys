package com.igarciamen.invitations.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "invitations",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_invitation_token", columnNames = "token"),
                @UniqueConstraint(name = "uk_invitation_survey_email", columnNames = {"survey_id", "email"})
        }
)
public class Invitation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // Cross-service reference (surveysdb). Plain Long, no FK across services.
    @Column(name = "survey_id", nullable = false)
    private Long surveyId;

    // Denormalized for convenience (shown to the invitee without calling surveys).
    @Column(name = "survey_title")
    private String surveyTitle;

    // The survey author who created the invitation (from the JWT).
    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "email", nullable = false)
    private String email;

    // Unique secret used to build the invite link.
    @Column(name = "token", nullable = false, length = 64)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    // The user who actually answered using this invitation (from the JWT).
    @Column(name = "respondent_user_id")
    private Long respondentUserId;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSurveyId() { return surveyId; }
    public void setSurveyId(Long surveyId) { this.surveyId = surveyId; }
    public String getSurveyTitle() { return surveyTitle; }
    public void setSurveyTitle(String surveyTitle) { this.surveyTitle = surveyTitle; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public InvitationStatus getStatus() { return status; }
    public void setStatus(InvitationStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Instant respondedAt) { this.respondedAt = respondedAt; }
    public Long getRespondentUserId() { return respondentUserId; }
    public void setRespondentUserId(Long respondentUserId) { this.respondentUserId = respondentUserId; }
}
