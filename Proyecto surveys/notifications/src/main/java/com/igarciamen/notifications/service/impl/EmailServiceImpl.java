package com.igarciamen.notifications.service.impl;

import com.igarciamen.notifications.payloads.request.InvitationEmailRequest;
import com.igarciamen.notifications.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    public EmailServiceImpl(JavaMailSender javaMailSender, TemplateEngine templateEngine) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendInvitationEmail(InvitationEmailRequest request) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(request.getTo());
            helper.setSubject("You have been invited to answer a survey");

            // Render the HTML body from the Thymeleaf template "invitation".
            Context context = new Context();
            context.setVariable("surveyTitle", request.getSurveyTitle());
            context.setVariable("inviteUrl", request.getInviteUrl());
            String htmlBody = templateEngine.process("invitation", context);

            helper.setText(htmlBody, true);
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Could not send invitation email: " + e.getMessage(), e);
        }
    }
}
