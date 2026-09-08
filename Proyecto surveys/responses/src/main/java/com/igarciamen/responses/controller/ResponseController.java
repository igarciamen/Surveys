package com.igarciamen.responses.controller;

import com.igarciamen.responses.model.SurveyResponse;
import com.igarciamen.responses.payloads.request.SubmissionRequest;
import com.igarciamen.responses.payloads.response.SurveyResultsDto;
import com.igarciamen.responses.service.ResponseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/responses")
public class ResponseController {

    private final ResponseService service;

    public ResponseController(ResponseService service) {
        this.service = service;
    }

    @Operation(summary = "Submit a response to a survey", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(path = "/survey/{surveyId}",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SurveyResponse> submit(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long surveyId,
            @Valid @RequestBody SubmissionRequest request) {
        SurveyResponse saved = service.submit(surveyId, request, userIdOrNull(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Get my submission for a survey", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(path = "/survey/{surveyId}/mine", produces = MediaType.APPLICATION_JSON_VALUE)
    public SurveyResponse mine(@AuthenticationPrincipal Jwt jwt, @PathVariable Long surveyId) {
        return service.getMine(surveyId, userId(jwt));
    }

    @Operation(summary = "Aggregated results (owner or admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(path = "/survey/{surveyId}/results", produces = MediaType.APPLICATION_JSON_VALUE)
    public SurveyResultsDto results(@AuthenticationPrincipal Jwt jwt, @PathVariable Long surveyId) {
        return service.results(surveyId);
    }


    @Operation(summary = "Submission timestamps (owner or admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(path = "/survey/{surveyId}/timestamps", produces = MediaType.APPLICATION_JSON_VALUE)
    public java.util.List<java.time.Instant> timestamps(@AuthenticationPrincipal Jwt jwt, @PathVariable Long surveyId) {
        return service.timestamps(surveyId);
    }



    // ---------- helpers ----------

    private Long userId(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        return ((Number) claim).longValue();
    }

    // Returns the user id, or null when the request is anonymous (no token).
    private Long userIdOrNull(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        Object claim = jwt.getClaim("userId");
        return claim == null ? null : ((Number) claim).longValue();
    }

    @Operation(summary = "Export responses as CSV (owner or admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(path = "/survey/{surveyId}/export/csv")
    public ResponseEntity<byte[]> exportCsv(@AuthenticationPrincipal Jwt jwt, @PathVariable Long surveyId) {
        byte[] data = service.exportCsv(surveyId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"survey-" + surveyId + "-responses.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(data);
    }

    @Operation(summary = "Export responses as Excel (owner or admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(path = "/survey/{surveyId}/export/xlsx")
    public ResponseEntity<byte[]> exportXlsx(@AuthenticationPrincipal Jwt jwt, @PathVariable Long surveyId) {
        byte[] data = service.exportXlsx(surveyId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"survey-" + surveyId + "-responses.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

}
