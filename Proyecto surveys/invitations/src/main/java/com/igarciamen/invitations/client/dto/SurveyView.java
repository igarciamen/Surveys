package com.igarciamen.invitations.client.dto;

public class SurveyView {
    private Long id;
    private String title;
    private String status;
    private String access;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAccess() { return access; }
    public void setAccess(String access) { this.access = access; }
}
