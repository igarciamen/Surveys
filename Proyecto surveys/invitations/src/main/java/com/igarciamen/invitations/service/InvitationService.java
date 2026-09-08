package com.igarciamen.invitations.service;

import com.igarciamen.invitations.client.NotificationClient;
import com.igarciamen.invitations.client.SurveyClient;
import com.igarciamen.invitations.client.dto.SurveyView;
import com.igarciamen.invitations.model.Invitation;
import com.igarciamen.invitations.model.InvitationStatus;
import com.igarciamen.invitations.repository.InvitationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class InvitationService {

    private final InvitationRepository repo;
    private final SurveyClient surveys;
    private final NotificationClient notifications;
    private final String frontendBaseUrl;


    public InvitationService(InvitationRepository repo, SurveyClient surveys,
                             NotificationClient notifications,
                             @Value("${frontend.base-url}") String frontendBaseUrl) {
        this.repo = repo;
        this.surveys = surveys;
        this.notifications = notifications;
        this.frontendBaseUrl = frontendBaseUrl;
    }


    // ---------------- Create ----------------

    @Transactional
    public List<Invitation> create(Long surveyId, List<String> rawEmails, Long ownerUserId) {
        // Ownership + title (403 if not owner/admin, 404 if missing).
        SurveyView survey = surveys.getManaged(surveyId);

        // Clean and de-duplicate the incoming emails.
        Set<String> emails = new LinkedHashSet<>();
        for (String e : rawEmails) {
            if (e == null) continue;
            String clean = e.trim().toLowerCase();
            if (!clean.isEmpty() && clean.contains("@")) {
                emails.add(clean);
            }
        }
        if (emails.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide at least one valid email");
        }

        for (String email : emails) {
            // Skip emails already invited to this survey.
            if (repo.existsBySurveyIdAndEmail(surveyId, email)) {
                continue;
            }
            Invitation inv = new Invitation();
            inv.setSurveyId(surveyId);
            inv.setSurveyTitle(survey.getTitle());
            inv.setOwnerUserId(ownerUserId);
            inv.setEmail(email);
            inv.setToken(UUID.randomUUID().toString().replace("-", ""));
            inv.setStatus(InvitationStatus.PENDING);
            repo.save(inv);
// Best-effort: si el correo falla, la invitación se crea igualmente.
            try {
                String link = frontendBaseUrl + "/surveys/" + surveyId + "/answer?invite=" + inv.getToken();
                notifications.sendInvitation(inv.getEmail(), survey.getTitle(), link);
            } catch (Exception ex) {
                // el email es opcional; no bloquea la creación
            }

        }

        return repo.findBySurveyIdOrderByCreatedAtAsc(surveyId);
    }

    // ---------------- List ----------------

    public List<Invitation> listForSurvey(Long surveyId) {
        surveys.getManaged(surveyId); // owner/admin only (403 otherwise)
        return repo.findBySurveyIdOrderByCreatedAtAsc(surveyId);
    }

    // ---------------- Token validation ----------------

    public Invitation getByToken(String token) {
        return repo.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
    }

    // ---------------- Accept (mark responded) ----------------

    @Transactional
    public Invitation accept(String token, Long respondentUserId) {
        Invitation inv = getByToken(token);
        if (inv.getStatus() != InvitationStatus.RESPONDED) {
            inv.setStatus(InvitationStatus.RESPONDED);
            inv.setRespondedAt(Instant.now());
            inv.setRespondentUserId(respondentUserId);
            repo.save(inv);
        }
        return inv;
    }

    // ---------------- Delete ----------------

    @Transactional
    public void delete(Long invitationId, Long requesterUserId) {
        Invitation inv = repo.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
        surveys.getManaged(inv.getSurveyId()); // owner/admin only (403 otherwise)
        repo.delete(inv);
    }
}
