package com.igarciamen.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GatewayRoutesTest {

    @Autowired
    RouteLocator routeLocator;

    @Test
    void contextLoadsAndAllExpectedRoutesAreConfigured() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertThat(routes).isNotNull();

        List<String> ids = routes.stream().map(Route::getId).toList();
        assertThat(ids).contains(
                "users-auth", "users", "surveys",
                "responses", "statistics", "invitations");
    }

    @Test
    void surveysRouteForwardsToTheSurveysService() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertThat(routes).isNotNull();

        Route surveys = routes.stream()
                .filter(r -> "surveys".equals(r.getId()))
                .findFirst().orElseThrow();
        assertThat(surveys.getUri().toString()).contains("8082");
    }
}
