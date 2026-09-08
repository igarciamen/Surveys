package com.igarciamen.notifications.service;

import com.igarciamen.notifications.payloads.request.InvitationEmailRequest;

public interface EmailService {

    // Contract: send the invitation email (recipient, survey title and unique link).
    void sendInvitationEmail(InvitationEmailRequest request);
}
