package com.igarciamen.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Static routing table: each /api/<area>/** prefix is forwarded, unchanged,
 * to the microservice that owns it. The downstream services already expose
 * their endpoints under /api/..., so no path rewriting is needed.
 * The Authorization header is forwarded automatically, so JWT keeps working.
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routes(
            RouteLocatorBuilder builder,
            @Value("${services.users-url}") String users,
            @Value("${services.surveys-url}") String surveys,
            @Value("${services.responses-url}") String responses,
            @Value("${services.statistics-url}") String statistics,
            @Value("${services.invitations-url}") String invitations) {

        return builder.routes()


                .route("users-auth", r -> r.path("/api/auth/**").uri(users))
                .route("users", r -> r.path("/api/user/**").uri(users))
                .route("users-plural", r -> r.path("/api/users/**").uri(users))


                .route("surveys", r -> r.path("/api/surveys/**").uri(surveys))
                .route("responses", r -> r.path("/api/responses/**").uri(responses))
                .route("statistics", r -> r.path("/api/statistics/**").uri(statistics))
                .route("invitations", r -> r.path("/api/invitations/**").uri(invitations))
                .build();
    }
}
