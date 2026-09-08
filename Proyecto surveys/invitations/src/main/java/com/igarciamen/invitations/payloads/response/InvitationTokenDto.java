package com.igarciamen.invitations.payloads.response;

import com.igarciamen.invitations.model.Invitation;

public class InvitationTokenDto {
    private Long surveyId;
    private String surveyTitle;
    private String email;
    private String status;

    public static InvitationTokenDto from(Invitation i) {
        InvitationTokenDto dto = new InvitationTokenDto();
        dto.surveyId = i.getSurveyId();
        dto.surveyTitle = i.getSurveyTitle();
        dto.email = i.getEmail();
        dto.status = i.getStatus().name();
        return dto;
    }

    public Long getSurveyId() { return surveyId; }
    public String getSurveyTitle() { return surveyTitle; }
    public String getEmail() { return email; }
    public String getStatus() { return status; }
}
