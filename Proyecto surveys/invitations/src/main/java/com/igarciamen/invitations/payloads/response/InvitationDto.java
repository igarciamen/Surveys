package com.igarciamen.invitations.payloads.response;

import com.igarciamen.invitations.model.Invitation;

import java.time.Instant;

public class InvitationDto {
    private Long id;
    private Long surveyId;
    private String email;
    private String token;
    private String status;
    private Instant createdAt;
    private Instant respondedAt;

    public static InvitationDto from(Invitation i) {
        InvitationDto dto = new InvitationDto();
        dto.id = i.getId();
        dto.surveyId = i.getSurveyId();
        dto.email = i.getEmail();
        dto.token = i.getToken();
        dto.status = i.getStatus().name();
        dto.createdAt = i.getCreatedAt();
        dto.respondedAt = i.getRespondedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getSurveyId() { return surveyId; }
    public String getEmail() { return email; }
    public String getToken() { return token; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getRespondedAt() { return respondedAt; }
}
