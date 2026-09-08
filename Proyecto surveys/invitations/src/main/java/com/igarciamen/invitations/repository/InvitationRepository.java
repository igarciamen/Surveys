package com.igarciamen.invitations.repository;

import com.igarciamen.invitations.model.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    List<Invitation> findBySurveyIdOrderByCreatedAtAsc(Long surveyId);

    Optional<Invitation> findByToken(String token);

    boolean existsBySurveyIdAndEmail(Long surveyId, String email);
}
