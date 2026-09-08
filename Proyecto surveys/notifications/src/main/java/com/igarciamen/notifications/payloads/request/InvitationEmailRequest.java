package com.igarciamen.notifications.payloads.request;

public class InvitationEmailRequest {
    private String to;            // recipient email
    private String surveyTitle;   // survey name shown in the email
    private String inviteUrl;     // unique invitation link

    public InvitationEmailRequest() { }

    public InvitationEmailRequest(String to, String surveyTitle, String inviteUrl) {
        this.to = to;
        this.surveyTitle = surveyTitle;
        this.inviteUrl = inviteUrl;
    }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    public String getSurveyTitle() { return surveyTitle; }
    public void setSurveyTitle(String surveyTitle) { this.surveyTitle = surveyTitle; }
    public String getInviteUrl() { return inviteUrl; }
    public void setInviteUrl(String inviteUrl) { this.inviteUrl = inviteUrl; }
}
