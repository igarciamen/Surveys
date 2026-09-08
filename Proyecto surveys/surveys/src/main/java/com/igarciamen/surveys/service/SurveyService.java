package com.igarciamen.surveys.service;

import com.igarciamen.surveys.model.Question;
import com.igarciamen.surveys.model.QuestionOption;
import com.igarciamen.surveys.model.Survey;
import com.igarciamen.surveys.model.SurveyAccess;
import com.igarciamen.surveys.model.SurveyStatus;
import com.igarciamen.surveys.repository.SurveyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

@Service
public class SurveyService {

    private final SurveyRepository repo;
    private final RestTemplate http;
    private final String usersBaseUrl;

    public SurveyService(SurveyRepository repo,
                         RestTemplate http,
                         @Value("${users.base-url}") String usersBaseUrl) {
        this.repo = repo;
        this.http = http;
        this.usersBaseUrl = usersBaseUrl;
    }

    // ---------- Public catalog ----------

    public Page<Survey> listPublished(String q, Pageable pageable) {
        Page<Survey> page = (q == null || q.isBlank())
                ? repo.findByStatus(SurveyStatus.PUBLISHED, pageable)
                : repo.searchPublished(q, pageable);
        page.forEach(this::attachOwnerSafe);
        return page;
    }

    public Survey getPublished(Long id) {
        Survey survey = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Survey not found: " + id));
        if (survey.getStatus() != SurveyStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Survey not available");
        }
        attachOwnerSafe(survey);
        return survey;
    }

    // ---------- Owner area ----------

    public Page<Survey> listMine(Long ownerId, Pageable pageable) {
        Page<Survey> page = repo.findByOwnerUserId(ownerId, pageable);
        page.forEach(this::attachOwnerSafe);
        return page;
    }

    public Survey getOwned(Long id, Long requesterId, boolean isAdmin) {
        Survey survey = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Survey not found: " + id));
        requireOwnerOrAdmin(survey, requesterId, isAdmin);
        attachOwnerSafe(survey);
        return survey;
    }

    @Transactional
    public Survey create(Survey input, Long ownerId) {
        input.setId(null);
        input.setOwnerUserId(ownerId);
        input.setStatus(SurveyStatus.DRAFT);
        input.setPublishedAt(null);
        wireChildren(input);
        Survey saved = repo.save(input);
        attachOwnerSafe(saved);
        return saved;
    }

    @Transactional
    public Survey update(Long id, Survey input, Long requesterId, boolean isAdmin) {
        Survey existing = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Survey not found: " + id));
        requireOwnerOrAdmin(existing, requesterId, isAdmin);
        if (existing.getStatus() != SurveyStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only DRAFT surveys can be edited");
        }
        existing.setTitle(input.getTitle());
        existing.setDescription(input.getDescription());
        existing.setAccess(input.getAccess());
        existing.setAccessPassword(input.getAccessPassword());
        existing.setClosesAt(input.getClosesAt());

        // Replace the whole question schema (orphanRemoval clears the old ones).
        existing.getQuestions().clear();
        if (input.getQuestions() != null) {
            input.getQuestions().forEach(existing.getQuestions()::add);
        }
        wireChildren(existing);

        Survey saved = repo.save(existing);
        attachOwnerSafe(saved);
        return saved;
    }

    @Transactional
    public Survey changeStatus(Long id, SurveyStatus target, Long requesterId, boolean isAdmin) {
        Survey survey = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Survey not found: " + id));
        requireOwnerOrAdmin(survey, requesterId, isAdmin);

        SurveyStatus current = survey.getStatus();
        boolean allowed =
                (current == SurveyStatus.DRAFT && target == SurveyStatus.PUBLISHED) ||
                        (current == SurveyStatus.PUBLISHED && target == SurveyStatus.CLOSED);
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Illegal transition: " + current + " -> " + target);
        }

        if (target == SurveyStatus.PUBLISHED) {
            if (survey.getQuestions().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A survey needs at least one question to be published");
            }
            survey.setPublishedAt(Instant.now());
        } else if (target == SurveyStatus.CLOSED) {
            survey.setClosesAt(Instant.now());
        }
        survey.setStatus(target);

        Survey saved = repo.save(survey);
        attachOwnerSafe(saved);
        return saved;
    }

    @Transactional
    public void delete(Long id, Long requesterId, boolean isAdmin) {
        Survey survey = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Survey not found: " + id));
        requireOwnerOrAdmin(survey, requesterId, isAdmin);
        repo.delete(survey);
    }

    // ---------- helpers ----------

    private void requireOwnerOrAdmin(Survey survey, Long requesterId, boolean isAdmin) {
        if (!isAdmin && !survey.getOwnerUserId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the owner of this survey");
        }
    }

    // Wire the back-references that Jackson left null (parent is @JsonIgnore).
    private void wireChildren(Survey survey) {
        if (survey.getQuestions() == null) return;
        int qPos = 0;
        for (Question q : survey.getQuestions()) {
            q.setSurvey(survey);
            q.setPosition(qPos++);
            if (q.getOptions() != null) {
                int oPos = 0;
                for (QuestionOption opt : q.getOptions()) {
                    opt.setQuestion(q);
                    opt.setPosition(oPos++);
                }
            }
        }
    }

    // Used by the responses service to validate the password of a PASSWORD survey.
    public boolean verifyPassword(Long id, String password) {
        Survey survey = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Survey not found: " + id));
        if (survey.getAccess() != SurveyAccess.PASSWORD) {
            return true; // the survey is not password-protected
        }
        return survey.getAccessPassword() != null
                && survey.getAccessPassword().equals(password);
    }

    @SuppressWarnings("unchecked")
    private void attachOwnerSafe(Survey survey) {
        try {
            Map<String, Object> user = http.getForObject(
                    usersBaseUrl + "/user/" + survey.getOwnerUserId(), Map.class);
            survey.setOwner(user);
        } catch (Exception ex) {
            survey.setOwner(null); // never let a downstream hiccup break the catalog
        }
    }
}
