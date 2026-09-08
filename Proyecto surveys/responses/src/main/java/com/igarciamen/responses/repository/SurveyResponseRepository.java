package com.igarciamen.responses.repository;

import com.igarciamen.responses.model.SurveyResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Long> {

    boolean existsBySurveyIdAndRespondentUserId(Long surveyId, Long respondentUserId);

    Optional<SurveyResponse> findBySurveyIdAndRespondentUserId(Long surveyId, Long respondentUserId);

    List<SurveyResponse> findBySurveyId(Long surveyId);

    long countBySurveyId(Long surveyId);
}
