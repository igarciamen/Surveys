package com.igarciamen.surveys.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http

                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/webjars/**"
                        ).permitAll()

                        // Public catalog: browse and read published surveys.
                        .requestMatchers(HttpMethod.GET, "/api/surveys").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/surveys/*").permitAll()

                        // Public: verify a survey access password (used to answer PASSWORD surveys)
                        .requestMatchers(HttpMethod.POST, "/api/surveys/*/verify-password").permitAll()

                        // The designer dashboard endpoints require authentication.
                        .requestMatchers(HttpMethod.GET, "/api/surveys/mine").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/surveys/*/manage").authenticated()

                        // Only designers (or admins) author surveys.
                        .requestMatchers(HttpMethod.POST, "/api/surveys").hasAnyAuthority("ROLE_DESIGNER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/surveys/*").hasAnyAuthority("ROLE_DESIGNER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/surveys/*/status").hasAnyAuthority("ROLE_DESIGNER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/surveys/*").hasAnyAuthority("ROLE_DESIGNER", "ROLE_ADMIN")

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(
                jwtSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    private JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter granted = new JwtGrantedAuthoritiesConverter();
        granted.setAuthoritiesClaimName("roles");
        granted.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(granted);
        return converter;
    }


}
