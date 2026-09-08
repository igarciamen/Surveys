package com.igarciamen.surveys.controller;

import com.igarciamen.surveys.model.Survey;
import com.igarciamen.surveys.model.SurveyStatus;
import com.igarciamen.surveys.service.SurveyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/surveys")
public class SurveyController {

    private final SurveyService service;

    public SurveyController(SurveyService service) {
        this.service = service;
    }

    // ---------- Public catalog ----------

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public PagedModel<Survey> list(
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 12) Pageable pageable) {
        return new PagedModel<>(service.listPublished(q, pageable));
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Survey getOne(@PathVariable Long id) {
        return service.getPublished(id);
    }

    // ---------- Designer area ----------

    @Operation(summary = "List my surveys", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(path = "/mine", produces = MediaType.APPLICATION_JSON_VALUE)
    public PagedModel<Survey> mine(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 12) Pageable pageable) {
        return new PagedModel<>(service.listMine(ownerId(jwt), pageable));
    }

    @Operation(summary = "Get a survey I own (any status)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(path = "/{id}/manage", produces = MediaType.APPLICATION_JSON_VALUE)
    public Survey manage(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.getOwned(id, ownerId(jwt), isAdmin(jwt));
    }

    @Operation(summary = "Create a survey", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Survey> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Survey survey) {
        Survey created = service.create(survey, ownerId(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update a DRAFT survey", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Survey update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody Survey survey) {
        return service.update(id, survey, ownerId(jwt), isAdmin(jwt));
    }

    @Operation(summary = "Change survey status (publish / close)", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping(path = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Survey changeStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        SurveyStatus target = SurveyStatus.valueOf(body.get("status"));
        return service.changeStatus(id, target, ownerId(jwt), isAdmin(jwt));
    }

    @Operation(summary = "Delete a survey", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(id, ownerId(jwt), isAdmin(jwt));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Verify the access password of a survey")
    @PostMapping(path = "/{id}/verify-password",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Boolean> verifyPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        boolean valid = service.verifyPassword(id, body.get("password"));
        return Map.of("valid", valid);
    }

    // ---------- helpers ----------

    private Long ownerId(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        return ((Number) claim).longValue();
    }

    private boolean isAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("ROLE_ADMIN");
    }
}
