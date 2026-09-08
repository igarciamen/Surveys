package com.igarciamen.responses.service;

import com.igarciamen.responses.client.SurveyClient;
import com.igarciamen.responses.client.dto.OptionSchemaDto;
import com.igarciamen.responses.client.dto.QuestionSchemaDto;
import com.igarciamen.responses.client.dto.SurveySchemaDto;
import com.igarciamen.responses.model.Answer;
import com.igarciamen.responses.model.SurveyResponse;
import com.igarciamen.responses.payloads.request.AnswerInput;
import com.igarciamen.responses.payloads.request.SubmissionRequest;
import com.igarciamen.responses.payloads.response.OptionCountDto;
import com.igarciamen.responses.payloads.response.QuestionResultDto;
import com.igarciamen.responses.payloads.response.SurveyResultsDto;
import com.igarciamen.responses.repository.SurveyResponseRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ResponseService {

    private static final Set<String> SINGLE_SELECT = Set.of("SINGLE_CHOICE", "DROPDOWN", "RATING");
    private static final Set<String> MULTI_SELECT = Set.of("MULTIPLE_CHOICE");
    private static final Set<String> OPTION_TYPES =
            Set.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "DROPDOWN", "RATING");

    private final SurveyResponseRepository repo;
    private final SurveyClient surveys;

    public ResponseService(SurveyResponseRepository repo, SurveyClient surveys) {
        this.repo = repo;
        this.surveys = surveys;
    }

    // ---------------- Submit ----------------

    @Transactional
    public SurveyResponse submit(Long surveyId, SubmissionRequest request, Long respondentUserId) {

        // 1) Fetch the published schema (404 if not published / not found). Includes the access mode.
        SurveySchemaDto schema = surveys.getPublished(surveyId);

        // 2) Enforce the access mode of the survey.
        String access = schema.getAccess();
        if ("RESTRICTED".equals(access) && respondentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "This survey is restricted to registered users; please log in");
        }
        if ("PASSWORD".equals(access)) {
            if (respondentUserId == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "This survey requires a registered account; please log in");
            }
            String pwd = request.getAccessPassword();
            if (pwd == null || pwd.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This survey requires a password");
            }
            if (!surveys.verifyPassword(surveyId, pwd)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incorrect survey password");
            }
        }
        // OPEN: anyone may answer, even anonymously.

        // 3) One submission per REGISTERED user. Anonymous submissions are not de-duplicated.
        if (respondentUserId != null
                && repo.existsBySurveyIdAndRespondentUserId(surveyId, respondentUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already answered this survey");
        }

        // 4) Validate the submission against the schema.
        validate(schema, request);

        // 5) Build and persist.
        SurveyResponse response = new SurveyResponse();
        response.setSurveyId(surveyId);
        response.setRespondentUserId(respondentUserId);

        for (AnswerInput in : request.getAnswers()) {
            if (!isAnswered(in)) {
                continue; // skip empty optional answers
            }
            Answer answer = new Answer();
            answer.setResponse(response);
            answer.setQuestionId(in.getQuestionId());
            answer.setTextValue(trimToNull(in.getTextValue()));
            answer.setSelectedOptionIds(new ArrayList<>(new LinkedHashSet<>(in.getSelectedOptionIds())));
            response.getAnswers().add(answer);
        }

        try {
            return repo.save(response);
        } catch (DataIntegrityViolationException ex) {
            // Race condition against the unique constraint.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already answered this survey");
        }
    }

    private void validate(SurveySchemaDto schema, SubmissionRequest request) {
        Map<Long, QuestionSchemaDto> questionById = schema.getQuestions().stream()
                .collect(Collectors.toMap(QuestionSchemaDto::getId, Function.identity()));

        Set<Long> seen = new LinkedHashSet<>();

        for (AnswerInput in : request.getAnswers()) {
            Long qid = in.getQuestionId();
            QuestionSchemaDto q = questionById.get(qid);
            if (q == null) {
                throw bad("Question " + qid + " does not belong to this survey");
            }
            if (!seen.add(qid)) {
                throw bad("Duplicated answer for question " + qid);
            }
            if (!isAnswered(in)) {
                continue; // empty optional answer; required check happens below
            }
            validateAnswerType(q, in);
        }

        // Required questions must be answered.
        for (QuestionSchemaDto q : schema.getQuestions()) {
            if (!q.isRequired()) continue;
            AnswerInput in = request.getAnswers().stream()
                    .filter(a -> q.getId().equals(a.getQuestionId()))
                    .findFirst().orElse(null);
            if (in == null || !isAnswered(in)) {
                throw bad("Question \"" + q.getLabel() + "\" is required");
            }
        }
    }

    private void validateAnswerType(QuestionSchemaDto q, AnswerInput in) {
        String type = q.getType();

        if (OPTION_TYPES.contains(type)) {
            Set<Long> valid = q.getOptions().stream()
                    .map(OptionSchemaDto::getId).collect(Collectors.toSet());
            List<Long> chosen = in.getSelectedOptionIds();
            for (Long oid : chosen) {
                if (!valid.contains(oid)) {
                    throw bad("Option " + oid + " is not valid for question \"" + q.getLabel() + "\"");
                }
            }
            if (SINGLE_SELECT.contains(type) && chosen.size() > 1) {
                throw bad("Question \"" + q.getLabel() + "\" allows only one option");
            }
            return;
        }

        String text = trimToNull(in.getTextValue());
        switch (type) {
            case "NUMBER" -> {
                if (text != null && !isNumber(text)) {
                    throw bad("Question \"" + q.getLabel() + "\" expects a number");
                }
            }
            case "DATE" -> {
                if (text != null && !isDate(text)) {
                    throw bad("Question \"" + q.getLabel() + "\" expects a date (YYYY-MM-DD)");
                }
            }
            case "BOOLEAN" -> {
                if (text != null && !"true".equalsIgnoreCase(text) && !"false".equalsIgnoreCase(text)) {
                    throw bad("Question \"" + q.getLabel() + "\" expects true or false");
                }
            }
            default -> { /* TEXT / TEXTAREA: free text, nothing to check */ }
        }
    }

    // ---------------- My submission ----------------

    public SurveyResponse getMine(Long surveyId, Long respondentUserId) {
        return repo.findBySurveyIdAndRespondentUserId(surveyId, respondentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "You have not answered this survey yet"));
    }

    // ---------------- Aggregated results ----------------

    public SurveyResultsDto results(Long surveyId) {
        // getManaged enforces owner/admin via the surveys service (403 otherwise).
        SurveySchemaDto schema = surveys.getManaged(surveyId);

        List<SurveyResponse> all = repo.findBySurveyId(surveyId);

        SurveyResultsDto out = new SurveyResultsDto();
        out.setSurveyId(surveyId);
        out.setTitle(schema.getTitle());
        out.setTotalResponses(all.size());

        // Flatten answers grouped by questionId.
        Map<Long, List<Answer>> answersByQuestion = all.stream()
                .flatMap(r -> r.getAnswers().stream())
                .collect(Collectors.groupingBy(Answer::getQuestionId));

        for (QuestionSchemaDto q : schema.getQuestions()) {
            List<Answer> answers = answersByQuestion.getOrDefault(q.getId(), List.of());
            out.getQuestions().add(aggregateQuestion(q, answers));
        }
        return out;
    }

    private QuestionResultDto aggregateQuestion(QuestionSchemaDto q, List<Answer> answers) {
        QuestionResultDto qr = new QuestionResultDto();
        qr.setQuestionId(q.getId());
        qr.setLabel(q.getLabel());
        qr.setType(q.getType());
        qr.setTotalAnswered(answers.size());

        String type = q.getType();

        if (OPTION_TYPES.contains(type)) {
            List<OptionCountDto> counts = new ArrayList<>();
            for (OptionSchemaDto opt : q.getOptions()) {
                long c = answers.stream()
                        .filter(a -> a.getSelectedOptionIds().contains(opt.getId()))
                        .count();
                counts.add(new OptionCountDto(opt.getId(), opt.getLabel(), c));
            }
            qr.setOptions(counts);

            if ("RATING".equals(type)) {
                // Weighted average using each option's numeric value (or its label).
                double sum = 0; long n = 0;
                for (OptionSchemaDto opt : q.getOptions()) {
                    Double v = parseNumber(opt.getValue() != null ? opt.getValue() : opt.getLabel());
                    if (v == null) continue;
                    long c = counts.stream().filter(o -> o.getOptionId().equals(opt.getId()))
                            .mapToLong(OptionCountDto::getCount).findFirst().orElse(0);
                    sum += v * c; n += c;
                }
                qr.setAverage(n == 0 ? null : round(sum / n));
            }
        } else if ("BOOLEAN".equals(type)) {
            long t = answers.stream().filter(a -> "true".equalsIgnoreCase(a.getTextValue())).count();
            long f = answers.stream().filter(a -> "false".equalsIgnoreCase(a.getTextValue())).count();
            qr.setTrueCount(t);
            qr.setFalseCount(f);
        } else if ("NUMBER".equals(type)) {
            List<Double> nums = answers.stream()
                    .map(a -> parseNumber(a.getTextValue()))
                    .filter(java.util.Objects::nonNull)
                    .toList();
            qr.setAverage(nums.isEmpty() ? null
                    : round(nums.stream().mapToDouble(Double::doubleValue).average().orElse(0)));
        } else {
            // TEXT / TEXTAREA / DATE: keep a small sample of values.
            List<String> samples = answers.stream()
                    .map(Answer::getTextValue)
                    .filter(v -> v != null && !v.isBlank())
                    .limit(5)
                    .collect(Collectors.toList());
            qr.setSamples(samples);
        }
        return qr;
    }


    public java.util.List<java.time.Instant> timestamps(Long surveyId) {
        surveys.getManaged(surveyId); // 403 si no eres dueño/admin, 404 si la encuesta no existe
        return repo.findBySurveyId(surveyId).stream()
                .map(SurveyResponse::getSubmittedAt)
                .sorted()
                .toList();
    }






    // ---------------- helpers ----------------

    private boolean isAnswered(AnswerInput in) {
        boolean hasText = trimToNull(in.getTextValue()) != null;
        boolean hasOptions = in.getSelectedOptionIds() != null && !in.getSelectedOptionIds().isEmpty();
        return hasText || hasOptions;
    }

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean isNumber(String s) { return parseNumber(s) != null; }

    private static Double parseNumber(String s) {
        if (s == null) return null;
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static boolean isDate(String s) {
        try { LocalDate.parse(s.trim()); return true; } catch (Exception e) { return false; }
    }

    private static Double round(double v) { return Math.round(v * 100.0) / 100.0; }

    // ---------------- Export ----------------

    public byte[] exportCsv(Long surveyId) {
        ExportTable table = buildTable(surveyId);
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF'); // UTF-8 BOM so Excel detects the encoding
        appendCsvRow(sb, table.header());
        for (java.util.List<String> row : table.rows()) {
            appendCsvRow(sb, row);
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] exportXlsx(Long surveyId) {
        ExportTable table = buildTable(surveyId);
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("Responses");
            org.apache.poi.ss.usermodel.CellStyle headerStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            for (int col = 0; col < table.header().size(); col++) {
                org.apache.poi.ss.usermodel.Cell cell = header.createCell(col);
                cell.setCellValue(table.header().get(col));
                cell.setCellStyle(headerStyle);
            }
            int rowIdx = 1;
            for (java.util.List<String> row : table.rows()) {
                org.apache.poi.ss.usermodel.Row r = sheet.createRow(rowIdx++);
                for (int col = 0; col < row.size(); col++) {
                    r.createCell(col).setCellValue(row.get(col));
                }
            }
            for (int col = 0; col < table.header().size(); col++) {
                sheet.autoSizeColumn(col);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate the Excel file");
        }
    }

    private ExportTable buildTable(Long surveyId) {
        SurveySchemaDto schema = surveys.getManaged(surveyId); // owner/admin (403) + 404
        java.util.List<SurveyResponse> all = repo.findBySurveyId(surveyId);
        java.util.List<QuestionSchemaDto> questions = schema.getQuestions();

        java.util.List<String> header = new java.util.ArrayList<>();
        header.add("Response ID");
        header.add("Respondent");
        header.add("Submitted at");
        for (QuestionSchemaDto q : questions) {
            header.add(q.getLabel());
        }

        java.util.Map<Long, java.util.Map<Long, String>> optionLabels = new java.util.HashMap<>();
        for (QuestionSchemaDto q : questions) {
            java.util.Map<Long, String> labels = new java.util.HashMap<>();
            if (q.getOptions() != null) {
                for (OptionSchemaDto o : q.getOptions()) {
                    labels.put(o.getId(), o.getLabel());
                }
            }
            optionLabels.put(q.getId(), labels);
        }

        java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
        for (SurveyResponse r : all) {
            java.util.Map<Long, Answer> byQuestion = r.getAnswers().stream()
                    .collect(java.util.stream.Collectors.toMap(Answer::getQuestionId, a -> a, (a, b) -> a));
            java.util.List<String> row = new java.util.ArrayList<>();
            row.add(String.valueOf(r.getId()));
            row.add(r.getRespondentUserId() == null ? "anonymous" : "user " + r.getRespondentUserId());
            row.add(r.getSubmittedAt().toString());
            for (QuestionSchemaDto q : questions) {
                row.add(cellValue(q, byQuestion.get(q.getId()), optionLabels.get(q.getId())));
            }
            rows.add(row);
        }
        return new ExportTable(header, rows);
    }

    private String cellValue(QuestionSchemaDto q, Answer answer, java.util.Map<Long, String> labels) {
        if (answer == null) {
            return "";
        }
        if (OPTION_TYPES.contains(q.getType())) {
            return answer.getSelectedOptionIds().stream()
                    .map(id -> labels.getOrDefault(id, String.valueOf(id)))
                    .collect(java.util.stream.Collectors.joining("; "));
        }
        return answer.getTextValue() == null ? "" : answer.getTextValue();
    }

    private static void appendCsvRow(StringBuilder sb, java.util.List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(escapeCsv(cells.get(i)));
        }
        sb.append("\r\n");
    }

    private static String escapeCsv(String v) {
        if (v == null) return "";
        boolean needsQuotes = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        String s = v.replace("\"", "\"\"");
        return needsQuotes ? "\"" + s + "\"" : s;
    }

    // Simple holder for the export table.
    private record ExportTable(java.util.List<String> header, java.util.List<java.util.List<String>> rows) {}



}
