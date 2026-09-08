package com.igarciamen.statistics.client;

import com.igarciamen.statistics.client.dto.SurveyResultsView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Component
public class ResponsesClient {

    private final RestTemplate http;
    private final String responsesBaseUrl;

    public ResponsesClient(RestTemplate http,
                           @Value("${responses.base-url}") String responsesBaseUrl) {
        this.http = http;
        this.responsesBaseUrl = responsesBaseUrl;
    }

    /** Aggregated results (responses enforces owner/admin: 403 otherwise, 404 if missing). */
    public SurveyResultsView getResults(Long surveyId) {
        try {
            return http.getForObject(
                    responsesBaseUrl + "/responses/survey/" + surveyId + "/results",
                    SurveyResultsView.class);
        } catch (HttpStatusCodeException ex) {
            throw new ResponseStatusException(ex.getStatusCode(),
                    "Could not load results from responses service");
        }
    }

    /** Submission timestamps for the time series (owner/admin enforced downstream). */
    public List<Instant> getTimestamps(Long surveyId) {
        try {
            Instant[] arr = http.getForObject(
                    responsesBaseUrl + "/responses/survey/" + surveyId + "/timestamps",
                    Instant[].class);
            return arr == null ? List.of() : Arrays.asList(arr);
        } catch (HttpStatusCodeException ex) {
            throw new ResponseStatusException(ex.getStatusCode(),
                    "Could not load timestamps from responses service");
        }
    }
}
