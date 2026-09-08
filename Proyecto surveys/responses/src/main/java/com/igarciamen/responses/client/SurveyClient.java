package com.igarciamen.responses.client;

import com.igarciamen.responses.client.dto.SurveySchemaDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Component
public class SurveyClient {

    private final RestTemplate http;
    private final String surveysBaseUrl;

    public SurveyClient(RestTemplate http,
                        @Value("${surveys.base-url}") String surveysBaseUrl) {
        this.http = http;
        this.surveysBaseUrl = surveysBaseUrl;
    }

    /**
     * Public detail endpoint: returns the survey only if it is PUBLISHED.
     * Used when accepting a submission (a survey must be published to be answered).
     */
    public SurveySchemaDto getPublished(Long surveyId) {
        try {
            return http.getForObject(surveysBaseUrl + "/surveys/" + surveyId, SurveySchemaDto.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Survey not available for answering");
        }
    }

    /**
     * Owner endpoint: returns the survey in any status, but only to its owner or an admin.
     * Used for results; the surveys service enforces ownership and returns 403 otherwise.
     */
    public SurveySchemaDto getManaged(Long surveyId) {
        try {
            return http.getForObject(surveysBaseUrl + "/surveys/" + surveyId + "/manage", SurveySchemaDto.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Survey not found: " + surveyId);
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view these results");
        }
    }

    /**
     * Verifies a survey access password. Public endpoint in surveys; returns true/false.
     */
    @SuppressWarnings("unchecked")
    public boolean verifyPassword(Long surveyId, String password) {
        Map<String, Object> body = Map.of("password", password == null ? "" : password);
        try {
            Map<String, Object> res = http.postForObject(
                    surveysBaseUrl + "/surveys/" + surveyId + "/verify-password", body, Map.class);
            return res != null && Boolean.TRUE.equals(res.get("valid"));
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Survey not available");
        }
    }
}
