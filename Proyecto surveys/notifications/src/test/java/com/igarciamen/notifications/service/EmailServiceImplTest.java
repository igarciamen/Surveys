package com.igarciamen.notifications.service;

import com.igarciamen.notifications.payloads.request.InvitationEmailRequest;
import com.igarciamen.notifications.service.impl.EmailServiceImpl;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    JavaMailSender javaMailSender;

    @Mock
    TemplateEngine templateEngine;

    @InjectMocks
    EmailServiceImpl emailService;

    @Test
    void sendsInvitationEmailThroughTheMailSender() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("invitation"), any(Context.class)))
                .thenReturn("<html>invitation</html>");

        emailService.sendInvitationEmail(
                new InvitationEmailRequest("alice@example.com", "My survey",
                        "http://localhost:4200/surveys/1/answer?invite=abc"));

        // The template is rendered and the message is actually sent.
        verify(templateEngine).process(eq("invitation"), any(Context.class));
        verify(javaMailSender).send(mimeMessage);
    }
}
