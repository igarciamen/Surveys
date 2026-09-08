package com.igarciamen.surveys.scheduling;

import com.igarciamen.surveys.model.Survey;
import com.igarciamen.surveys.model.SurveyStatus;
import com.igarciamen.surveys.repository.SurveyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class SurveyAutoCloseTask {

    private static final Logger log = LoggerFactory.getLogger(SurveyAutoCloseTask.class);

    private final SurveyRepository repo;

    public SurveyAutoCloseTask(SurveyRepository repo) {
        this.repo = repo;
    }

    /**
     * Periodically closes PUBLISHED surveys whose closing date has already passed.
     * Surveys without a closesAt (null) never match, so they stay open.
     * Interval and initial delay are configurable; sensible defaults are provided.
     */
    @Scheduled(
            initialDelayString = "${surveys.autoclose.initial-delay-ms:10000}",
            fixedDelayString = "${surveys.autoclose.interval-ms:60000}")
    @Transactional
    public void closeExpiredSurveys() {
        Instant now = Instant.now();
        List<Survey> expired = repo.findByStatusAndClosesAtBefore(SurveyStatus.PUBLISHED, now);
        if (expired.isEmpty()) {
            return;
        }
        for (Survey survey : expired) {
            survey.setStatus(SurveyStatus.CLOSED);
        }
        repo.saveAll(expired);
        log.info("Auto-closed {} expired survey(s) at {}", expired.size(), now);
    }
}
