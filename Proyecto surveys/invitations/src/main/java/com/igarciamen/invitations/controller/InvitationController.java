package com.igarciamen.invitations.controller;

import com.igarciamen.invitations.model.Invitation;
import com.igarciamen.invitations.payloads.request.CreateInvitationsRequest;
import com.igarciamen.invitations.payloads.response.InvitationDto;
import com.igarciamen.invitations.payloads.response.InvitationTokenDto;
import com.igarciamen.invitations.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final InvitationService service;

    public InvitationController(InvitationService service) {
        this.service = service;
    }

    @Operation(summary = "Create invitations for a survey (owner/admin)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(path = "/survey/{surveyId}",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<InvitationDto> create(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable Long surveyId,
                                      @RequestBody CreateInvitationsRequest request) {
        List<Invitation> all = service.create(surveyId, request.getEmails(), userId(jwt));
        return all.stream().map(InvitationDto::from).toList();
    }

    @Operation(summary = "List invitations of a survey (owner/admin)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(path = "/survey/{surveyId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<InvitationDto> list(@AuthenticationPrincipal Jwt jwt, @PathVariable Long surveyId) {
        return service.listForSurvey(surveyId).stream().map(InvitationDto::from).toList();
    }

    @Operation(summary = "Validate an invitation token", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(path = "/token/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    public InvitationTokenDto byToken(@AuthenticationPrincipal Jwt jwt, @PathVariable String token) {
        return InvitationTokenDto.from(service.getByToken(token));
    }

    @Operation(summary = "Mark an invitation as responded", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(path = "/token/{token}/accept", produces = MediaType.APPLICATION_JSON_VALUE)
    public InvitationTokenDto accept(@AuthenticationPrincipal Jwt jwt, @PathVariable String token) {
        return InvitationTokenDto.from(service.accept(token, userId(jwt)));
    }

    @Operation(summary = "Delete an invitation (owner/admin)", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping(path = "/{invitationId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long invitationId) {
        service.delete(invitationId, userId(jwt));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // ---------- helpers ----------

    private Long userId(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        return ((Number) claim).longValue();
    }
}
