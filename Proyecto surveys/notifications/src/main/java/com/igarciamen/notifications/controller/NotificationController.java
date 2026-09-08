package com.igarciamen.notifications.controller;

import com.igarciamen.notifications.payloads.request.InvitationEmailRequest;
import com.igarciamen.notifications.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final EmailService emailService;

    public NotificationController(EmailService emailService) {
        this.emailService = emailService;
    }

    @Operation(summary = "Send an invitation email (internal: called by the invitations service)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(path = "/invitation",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> sendInvitation(@RequestBody InvitationEmailRequest request) {
        emailService.sendInvitationEmail(request);
        return ResponseEntity.ok("Invitation email sent");
    }
}
