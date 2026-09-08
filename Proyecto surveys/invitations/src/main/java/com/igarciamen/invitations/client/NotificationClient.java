package com.igarciamen.invitations.client;
 
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
 
import java.util.Map;
 
@Component
public class NotificationClient {
 
    private final RestTemplate http;
    private final String notificationsBaseUrl;
 
    public NotificationClient(RestTemplate http,
                              @Value("${notifications.base-url}") String notificationsBaseUrl) {
        this.http = http;
        this.notificationsBaseUrl = notificationsBaseUrl;
    }
 
    /** Asks the notifications service to send the invitation email (forwards the caller's token). */
    public void sendInvitation(String to, String surveyTitle, String inviteUrl) {
        Map<String, String> body = Map.of(
                "to", to,
                "surveyTitle", surveyTitle,
                "inviteUrl", inviteUrl);
        http.postForEntity(notificationsBaseUrl + "/notifications/invitation", body, String.class);
    }
}
