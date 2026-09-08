package com.igarciamen.invitations.client;

import com.igarciamen.invitations.client.dto.SurveyView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SurveyClient {

    private final RestTemplate http;
    private final String surveysBaseUrl;

    public SurveyClient(RestTemplate http, @Value("${surveys.base-url}") String surveysBaseUrl) {
        this.http = http;
        this.surveysBaseUrl = surveysBaseUrl;
    }

    /**
     * Owner/admin view of a survey. Enforces ownership in the surveys service
     * (403 if you are not the owner/admin, 404 if it does not exist).
     */
    public SurveyView getManaged(Long surveyId) {
        try {
            return http.getForObject(surveysBaseUrl + "/surveys/" + surveyId + "/manage", SurveyView.class);
        } catch (HttpStatusCodeException ex) {
            throw new ResponseStatusException(ex.getStatusCode(), "Survey not accessible");
        }
    }
}
