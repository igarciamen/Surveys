package com.igarciamen.invitations.payloads.request;

import java.util.ArrayList;
import java.util.List;

public class CreateInvitationsRequest {
    private List<String> emails = new ArrayList<>();

    public List<String> getEmails() { return emails; }
    public void setEmails(List<String> emails) {
        this.emails = (emails == null) ? new ArrayList<>() : emails;
    }
}
